/*
 * Copyright 2017, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.retrostore.rpc.api;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.retrostore.client.common.ListAppsApiParams;
import org.retrostore.client.common.proto.ApiResponseApps;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.MediaType;
import org.retrostore.data.app.AppManagement;
import org.retrostore.data.app.AppStoreItem;
import org.retrostore.request.RequestData;
import org.retrostore.request.Responder;
import org.retrostore.request.Response;
import org.retrostore.resources.ImageServiceWrapper;
import org.retrostore.rpc.internal.ApiCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API call to list apps from the store.
 */
public class ListAppsApiCall implements ApiCall {
  private static final Logger LOG = Logger.getLogger("ListAppsApiCall");
  private final AppManagement mAppManagement;
  private final ApiHelper mApiHelper;

  public ListAppsApiCall(AppManagement appManagement, ImageServiceWrapper imageService) {
    mAppManagement = appManagement;
    mApiHelper = new ApiHelper(appManagement, imageService);
  }

  @Override
  public String getName() {
    return "listApps";
  }

  @Override
  public Response call(final RequestData data) {
    final ApiResponseApps responseApps = callInternal(data);
    return responder -> responder.respondProto(responseApps);
  }

  private ApiResponseApps callInternal(RequestData data) {
    long tStart = System.currentTimeMillis();
    ApiResponseApps.Builder response = ApiResponseApps.newBuilder();
    ListAppsApiParams params = parseParams(data.getBody());
    if (params == null) {
      return response.setSuccess(false).setMessage("Cannot parse parameters.").build();
    }

    // TODO: This is not efficient once we have a large number of apps. However, we currently
    // cache them all, so the appManagement implementation used here should be the caching kind.
    List<AppStoreItem> allApps = mAppManagement.getAllApps();
    if (allApps.size() - 1 < params.start) {
      return response.setSuccess(false).setMessage("Parameter 'start' out of range").build();
    }

    long tPreBuilding = System.currentTimeMillis();
    LOG.info(String.format("[Perf] getAllApps took %d ms. ", (tPreBuilding - tStart)));
    List<AppStoreItem> filteredApps = filterApps(allApps, params);

    List<App.Builder> apps = new ArrayList<>();
    for (int i = params.start; i < params.start + params.num && i < filteredApps.size(); ++i) {
      AppStoreItem appStoreItem = filteredApps.get(i);
      apps.add(mApiHelper.convert(appStoreItem));
    }
    LOG.info(String.format("[Perf] Building list took %d ms.", (System
        .currentTimeMillis() - tPreBuilding)));

    // Sort the output alphabetically.
    apps.sort((o1, o2) -> {
      if (o1 == null || o2 == null) {
        return 0;
      }
      return o1.getName().compareTo(o2.getName());
    });
    for (App.Builder app : apps) {
      response.addApp(app.build());
    }
    return response.setSuccess(true).setMessage("All good :-)").build();
  }

  private List<AppStoreItem> filterApps(List<AppStoreItem> apps, ListAppsApiParams params) {
    // Get all IDs that match the search query. Keep the set null if no search query was given.
    Set<String> appIdsFromSearch = null;
    if (params.query != null && !params.query.trim().isEmpty()) {
      appIdsFromSearch = Sets.newHashSet(mAppManagement.searchApps(params.query));
    }

    // Filter the apps, ensure they match the search or other options.
    List<AppStoreItem> filtered = new ArrayList<>();
    for (AppStoreItem app : apps) {
      if (appIdsFromSearch != null && !appIdsFromSearch.contains(app.id)) {
        continue;
      }
      if (!matchesTrs80Filter(params.trs80, app)) {
        continue;
      }
      // The app passed all the tests and is therefore added to the list.
      filtered.add(app);
    }
    return filtered;
  }

  private ListAppsApiParams parseParams(String params) {
    try {
      return (new Gson()).fromJson(params, ListAppsApiParams.class);
    } catch (Exception ex) {
      LOG.log(Level.WARNING, "Cannot parse params", ex);
      return null;
    }
  }

  // TODO: This should become a filter on the data store some day.
  private boolean matchesTrs80Filter(ListAppsApiParams.Trs80Params params, AppStoreItem item) {
    if (params == null || params.mediaTypes == null || params.mediaTypes.isEmpty()) {
      return true;
    }
    AppStoreItem.Trs80Extension trs80 = item.trs80Extension;
    if (trs80 == null) {
      // If there is no extension, then there cannot be a TRS80 media type.
      return false;
    }

    for (String mediaTypeStr : params.mediaTypes) {
      MediaType mediaType = parse(mediaTypeStr);
      if (mediaType == null) {
        // An non-existent type cannot match.
        continue;
      }

      // Check whether this TRS80 extension has the asked-for media type.
      switch (mediaType) {
        case DISK:
          for (long diskIds : trs80.disk) {
            if (diskIds > 0) {
              return true;
            }
          }
          break;
        case CASSETTE:
          if (trs80.cassette > 0) {
            return true;
          }
          break;
        case COMMAND:
          if (trs80.command > 0) {
            return true;
          }
          break;
        case BASIC:
          if (trs80.basic > 0) {
            return true;
          }
          break;
      }
    }
    return false;
  }

  private static MediaType parse(String mediaTypeStr) {
    try {
      return MediaType.valueOf(mediaTypeStr);
    } catch (IllegalArgumentException ignore) {
      return null;
    }
  }
}

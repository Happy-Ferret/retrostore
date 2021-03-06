/*
 *  Copyright 2017, Sascha Häberling
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.retrostore.rpc;

import org.retrostore.data.app.AppManagement;
import org.retrostore.data.app.AppStoreItem;
import org.retrostore.data.app.Author;
import org.retrostore.data.app.MediaImage;
import org.retrostore.data.user.UserAccountType;
import org.retrostore.request.Responder;
import org.retrostore.resources.ImageServiceWrapper;
import org.retrostore.rpc.internal.RpcCall;
import org.retrostore.rpc.internal.RpcParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Returns a list of apps from the datastore that is used to show on the public website.
 */
public class PublicAppListRpcCall implements RpcCall<RpcParameters> {
  private static final int SCREENSHOT_SIZE = 800;
  private static final String FALLBACK_SCREENSHOT_URL = "/gfx/loading_failed.png";
  private final AppManagement mAppManagement;
  private final ImageServiceWrapper mImageService;

  public PublicAppListRpcCall(AppManagement appManagement, ImageServiceWrapper imageService) {
    mAppManagement = appManagement;
    mImageService = imageService;
  }

  @Override
  public String getName() {
    return "pubapplist";
  }

  @Override
  public boolean isPermitted(UserAccountType type) {
    // This is a public call, no login required.
    return true;
  }

  @Override
  public void call(RpcParameters params, Responder responder) {
    List<AppStoreItem> allApps = mAppManagement.getAllApps();

    List<PubAppListItem> listingApps = new ArrayList<>(allApps.size());
    // We should probably add a new class here which contains only the stuff we need. For now we
    // simply remove what we don't want to send, i.e. the disk contents.
    for (AppStoreItem app : allApps) {
      PubAppListItem listingApp = new PubAppListItem();
      listingApp.name = app.listing.name;
      listingApp.version = app.listing.versionString;
      listingApp.author = getAuthorString(app.listing.authorId);
      listingApp.description = app.listing.description;
      listingApp.screenshots = getScreenshotUrls(app.screenshotsBlobKeys);
      listingApp.reportUrl = "/reportapp?appId=" + app.id;
      listingApp.downloadUrl = "/downloadapp?appId=" + app.id;

      // Only set this if the app contains an image in a format which JS-TRS supports.
      // Note: CMD is supported, but some apps which then access a disk fail.
      if (hasDmkImage(app.id)) {
        listingApp.emulatorAppId = app.id;
      }
      listingApps.add(listingApp);
    }

    listingApps.sort((o1, o2) -> {
      if (o1 == null || o1.name == null || o2 == null) {
        return 0;
      }
      return o1.name.compareTo(o2.name);
    });
    responder.respondJson(listingApps);
  }

  private String getAuthorString(long id) {
    Optional<Author> author = mAppManagement.getAuthorById(id);
    if (author.isPresent()) {
      return author.get().name;
    }
    return "Unknown author";
  }

  private String[] getScreenshotUrls(List<String> blobkeys) {
    String[] urls = new String[blobkeys.size()];
    for (int i = 0; i < blobkeys.size(); ++i) {
      Optional<String> servingUrl = mImageService.getServingUrl(blobkeys.get(i), SCREENSHOT_SIZE);
      urls[i] = servingUrl.orElse(FALLBACK_SCREENSHOT_URL);
    }
    return urls;
  }

  class PubAppListItem {
    public String name;
    public String version;
    public String author;
    public String description;
    public String[] screenshots;
    public String reportUrl;
    public String downloadUrl;
    /** Sets to the appId if supported by the JS-TRS emulator. */
    public String emulatorAppId;
  }

  private boolean hasDmkImage(String appId) {
    Map<Long, MediaImage> mediaImages = mAppManagement.getMediaImagesForApp(appId);
    for (MediaImage media : mediaImages.values()) {
      if (media.filename.toLowerCase().endsWith(".dmk")) {
        return true;
      }
    }
    return false;
  }
}

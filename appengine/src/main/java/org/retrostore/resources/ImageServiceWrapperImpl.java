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

package org.retrostore.resources;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ServingUrlOptions;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Default implementation for accessing the image service.
 */
public class ImageServiceWrapperImpl implements ImageServiceWrapper {
  private static final Logger LOG = Logger.getLogger("ImageServiceImpl");

  private final ImagesService mImagesService;

  public ImageServiceWrapperImpl(ImagesService imagesService) {
    mImagesService = imagesService;
  }

  @Override
  public Optional<String> getServingUrl(String blobKey, int imageSize) {
    ServingUrlOptions options = ServingUrlOptions.Builder
        .withBlobKey(new BlobKey(blobKey))
        .secureUrl(true)
        .imageSize(imageSize);
    try {
      return Optional.of(mImagesService.getServingUrl(options));
    } catch (IllegalArgumentException ex) {
      LOG.warning("Cannot get image serving URL: " + ex.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Optional<String> getServingUrl(String blobKey) {
    return getServingUrl(blobKey, DEFAULT_SCREENSHOT_SIZE);
  }
}

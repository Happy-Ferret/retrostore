/*
 * Copyright 2017, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.retrostore.resources;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.CharStreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Functionality around files, e.g. loading them as resources.
 */
public class DefaultResourceLoader implements ResourceLoader {
  private static final Logger LOG = Logger.getLogger("DefaultResourceLoader");

  @Override
  public Optional<String> load(String filename) {
    // TODO: Cache!
    // TODO: Debug mode for local reloading
    try {
      File file = new File(filename);
      LOG.info("Loading file: " + file.getAbsolutePath());
      InputStream fileStream = new FileInputStream(file);
      String content = CharStreams.toString(new InputStreamReader(fileStream, Charsets.UTF_8));
      return Optional.of(content);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Cannot load file.", e);
    }
    return Optional.absent();
  }

  @Override
  public Optional<String> loadUrl(String urlStr) {
    try {
      URL url = new URL(urlStr);
      InputStream is = url.openStream();
      String content = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
      is.close();
      return Optional.of(content);
    } catch (MalformedURLException e) {
      LOG.log(Level.SEVERE, "Invalid URL", e);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Cannot read URL", e);
    }
    return Optional.absent();
  }
}
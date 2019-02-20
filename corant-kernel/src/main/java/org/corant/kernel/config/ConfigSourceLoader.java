/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.kernel.config;

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.StringUtils.defaultString;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.corant.shared.util.ObjectUtils;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-kernel
 *
 * @author bingo 上午10:52:25
 *
 */
public class ConfigSourceLoader {

  public static List<ConfigSource> load(ClassLoader classLoader, int ordinal, Predicate<URL> filter,
      String... classPaths) throws IOException {
    Set<URL> loadedUrls = new LinkedHashSet<>();
    for (String path : classPaths) {
      asList(classLoader.getResources(path)).stream().filter(filter).forEach(loadedUrls::add);
      if (Thread.currentThread().getContextClassLoader() != classLoader) {
        asList(Thread.currentThread().getContextClassLoader().getResources(path))
            .forEach(loadedUrls::add);
      }
    }
    return loadedUrls.stream().map(url -> load(url, ordinal)).filter(ObjectUtils::isNotNull)
        .collect(Collectors.toList());
  }

  public static List<ConfigSource> load(int ordinal, Predicate<URL> filter, String... filePaths)
      throws IOException {
    List<ConfigSource> sources = new ArrayList<>();
    for (String path : asSet(filePaths)) {
      File file = new File(path);
      if (file.canRead() && filter.test(file.toURI().toURL())) {
        sources.add(load(new File(path), ordinal));
      }
    }
    return sources;
  }

  static AbstractConfigSource load(File fo, int ordinal) throws IOException {
    if (fo != null && fo.canRead()) {
      String fileName = fo.getName().toLowerCase(Locale.ROOT);
      try (InputStream is = new FileInputStream(fo)) {
        if (fileName.endsWith(".properties")) {
          return new PropertiesConfigSource(fo.getCanonicalPath(), ordinal, is);
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
          return new YamlConfigSource(is, ordinal);
        } else if (fileName.endsWith(".json")) {
          return new JsonConfigSource(null, ordinal);
        } else if (fileName.endsWith(".xml")) {
          return new XmlConfigSource(null, ordinal);
        }
      } catch (IOException e) {
        throw e;
      }
    }
    return null;
  }

  static AbstractConfigSource load(URL resourceUrl, int ordinal) {
    String urlstr = defaultString(resourceUrl.getPath()).toLowerCase(Locale.ROOT);
    if (urlstr.endsWith(".properties")) {
      return new PropertiesConfigSource(resourceUrl, ordinal);
    } else if (urlstr.endsWith(".yml") || urlstr.endsWith(".yaml")) {
      return new YamlConfigSource(resourceUrl, ordinal);
    } else if (urlstr.endsWith(".json")) {
      return new JsonConfigSource(resourceUrl, ordinal);
    } else if (urlstr.endsWith(".xml")) {
      return new XmlConfigSource(resourceUrl, ordinal);
    }
    return null;
  }
}

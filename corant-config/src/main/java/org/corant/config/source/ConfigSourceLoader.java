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
package org.corant.config.source;

import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultTrim;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.corant.config.CorantConfigSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.URLResource;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:52:25
 *
 */
public class ConfigSourceLoader {

  public static List<ConfigSource> load(ClassLoader classLoader, int ordinal, Predicate<URL> filter,
      String... classPaths) throws IOException {
    Set<URI> loadedUrls = new LinkedHashSet<>();
    for (String path : classPaths) {
      streamOf(classLoader.getResources(path)).filter(filter).map(ConfigSourceLoader::toURI)
          .forEach(loadedUrls::add);
      if (Thread.currentThread().getContextClassLoader() != classLoader) {
        streamOf(Thread.currentThread().getContextClassLoader().getResources(path))
            .map(ConfigSourceLoader::toURI).forEach(loadedUrls::add);
      }
    }
    return loadedUrls.stream().map(uri -> load(toURL(uri), ordinal)).filter(Objects::isNotNull)
        .collect(Collectors.toList());
  }

  public static List<ConfigSource> load(int ordinal, Predicate<URL> filter, String... locations)
      throws IOException {
    List<ConfigSource> sources = new ArrayList<>();
    for (String path : setOf(locations)) {
      Resources.from(path).findFirst().flatMap(r -> load(filter, r, ordinal))
          .ifPresent(sources::add);
    }
    return sources;
  }

  static Optional<CorantConfigSource> load(Predicate<URL> filter, URLResource resource,
      int ordinal) {
    if (resource != null && filter.test(resource.getURL())) {
      String location = defaultTrim(resource.getURL().getPath());
      try (InputStream is = resource.openStream()) {
        if (location.endsWith(".properties")) {
          return Optional.of(new PropertiesConfigSource(location, ordinal, is));
        } else if (location.endsWith(".yml") || location.endsWith(".yaml")) {
          return Optional.of(new YamlConfigSource(is, ordinal));
        } else if (location.endsWith(".json")) {
          return Optional.of(new JsonConfigSource(null, ordinal));
        } else if (location.endsWith(".xml")) {
          return Optional.of(new XmlConfigSource(location, ordinal, is));
        }
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return Optional.empty();
  }

  static CorantConfigSource load(URL resourceUrl, int ordinal) {
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

  static URI toURI(URL url) {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static URL toURL(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

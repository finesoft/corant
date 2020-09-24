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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Maps.flatStringMap;
import static org.corant.shared.util.Objects.forceCast;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.corant.config.CorantConfigSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.yaml.snakeyaml.Yaml;

/**
 * corant-config
 *
 * @author bingo 上午10:10:30
 *
 */
public class YamlConfigSource extends CorantConfigSource {

  private static final long serialVersionUID = 8099900233760280030L;

  final Map<String, String> properties;

  YamlConfigSource(InputStream in, int ordinal) {
    Class<?> yamlCls = tryAsClass("org.yaml.snakeyaml.Yaml");
    if (yamlCls != null) {
      this.ordinal = ordinal;
      try {
        // Yaml yaml = forceCast(yamlCls.newInstance());//JDK8
        Yaml yaml = forceCast(yamlCls.getDeclaredConstructor().newInstance());// JDK9+
        properties = Collections.unmodifiableMap(flatStringMap(yaml.load(in), ".", 16));
        name = null;
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      name = null;
      this.ordinal = Integer.MIN_VALUE;
      properties = new HashMap<>();
    }
  }

  YamlConfigSource(String name, Map<String, ?> properties, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
    this.properties = Collections.unmodifiableMap(flatStringMap(properties, ".", 16));
  }

  YamlConfigSource(URL resourceUrl, int ordinal) {
    Class<?> yamlCls = tryAsClass("org.yaml.snakeyaml.Yaml");
    if (yamlCls != null) {
      this.ordinal = ordinal;
      try (InputStream in = resourceUrl.openStream()) {
        // Yaml yaml = forceCast(yamlCls.newInstance());//JDK8
        Yaml yaml = forceCast(yamlCls.getDeclaredConstructor().newInstance());// JDK9+
        properties = Collections.unmodifiableMap(flatStringMap(yaml.load(in), ".", 16));
        name = shouldNotNull(resourceUrl).toExternalForm();
      } catch (IOException | InstantiationException | IllegalAccessException
          | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
          | SecurityException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      name = null;
      this.ordinal = Integer.MIN_VALUE;
      properties = new HashMap<>();
    }
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}

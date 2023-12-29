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

import static org.corant.shared.util.Maps.flatStringMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * corant-config
 *
 * @author bingo 上午10:10:30
 */
public class YamlConfigSource extends AbstractCorantConfigSource {

  private static final long serialVersionUID = 8099900233760280030L;

  final Map<String, String> properties;

  YamlConfigSource(Resource resource, int ordinal) {
    // Class<?> yamlCls = tryAsClass("org.yaml.snakeyaml.Yaml");
    this.ordinal = ordinal;
    try (InputStream in = resource.openInputStream()) {
      // Yaml yaml = forceCast(yamlCls.newInstance());//JDK8
      // Yaml yaml = forceCast(yamlCls.getDeclaredConstructor().newInstance());// JDK9+

      // yaml is required from 2023-02-10 even though it has security issues
      Yaml yaml = new Yaml(new Constructor(Map.class, new LoaderOptions()));
      properties = Collections.unmodifiableMap(flatStringMap(yaml.load(in), ".", 16));
      name = resource.getLocation();
    } catch (IllegalArgumentException | SecurityException | IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  YamlConfigSource(String name, Map<String, ?> properties, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
    this.properties = Collections.unmodifiableMap(flatStringMap(properties, ".", 16));
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}

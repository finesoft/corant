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

import static org.corant.shared.util.MapUtils.toMap;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-kernel
 *
 * @author bingo 下午5:19:11
 *
 */
public class PropertiesConfigSource extends AbstractConfigSource {

  final Map<String, String> properties;

  PropertiesConfigSource(String name, int ordinal, InputStream is) throws IOException {
    this(name, ordinal, toMap(load(is)));
  }

  /**
   * @param properties
   */
  PropertiesConfigSource(String name, int ordinal, Map<String, String> properties) {
    super();
    this.name = name;
    this.ordinal = ordinal;
    if (properties != null) {
      this.properties = Collections.unmodifiableMap(properties);
    } else {
      this.properties = Collections.emptyMap();
    }
  }

  /**
   * @param resourceUrl
   * @param ordinal
   */
  PropertiesConfigSource(URL resourceUrl, int ordinal) {
    this(shouldNotNull(resourceUrl).toExternalForm(), ordinal, toMap(getProperties(resourceUrl)));
  }

  public static Properties getProperties(URL url) {
    try (InputStream in = url.openStream()) {
      Properties props = new Properties();
      props.load(in);
      return props;
    } catch (IOException e) {
      throw new CorantRuntimeException(e,
          "Load properties config source from '%s' occured an error!", url);
    }
  }

  static Properties load(InputStream is) throws IOException {
    Properties pops = new Properties();
    pops.load(is);
    return pops;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  AbstractConfigSource withProperties(Map<String, String> properties) {
    return new PropertiesConfigSource(getName(), getOrdinal(), properties);
  }
}

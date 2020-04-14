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
import static org.corant.shared.util.MapUtils.toMap;
import static org.corant.shared.util.StringUtils.asDefaultString;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.corant.config.CorantConfigSource;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-config
 *
 * @author bingo 下午5:19:11
 *
 */
public class PropertiesConfigSource extends CorantConfigSource {

  final Map<String, String> properties;

  /**
   * @param resourceUrl
   * @param ordinal
   */
  public PropertiesConfigSource(URL resourceUrl, int ordinal) {
    this(shouldNotNull(resourceUrl).toExternalForm(), ordinal, toMap(getProperties(resourceUrl)));
  }

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

  public static Properties getProperties(URL url) {
    try (InputStream is = url.openStream()) {
      return load(is);
    } catch (IOException e) {
      throw new CorantRuntimeException(e,
          "Load properties config source from '%s' occured an error!", url);
    }
  }

  static Properties load(InputStream is) throws IOException {
    Properties props = new Properties();
    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
    props.replaceAll((k, v) -> asDefaultString(v).replace("\\", "\\\\"));// TCK 2020-04-14
    return props;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}

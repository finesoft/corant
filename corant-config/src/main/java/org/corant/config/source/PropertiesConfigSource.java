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
import static org.corant.shared.util.Maps.toMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-config
 *
 * @author bingo 下午5:19:11
 *
 */
public class PropertiesConfigSource extends AbstractCorantConfigSource {

  private static final long serialVersionUID = -9141492489031571885L;

  final Map<String, String> properties;

  public PropertiesConfigSource(URL resourceUrl, int ordinal) {
    this(shouldNotNull(resourceUrl).toExternalForm(), ordinal, toMap(getProperties(resourceUrl)));
  }

  PropertiesConfigSource(String name, int ordinal, InputStream is) throws IOException {
    this(name, ordinal, toMap(load(is)));
  }

  PropertiesConfigSource(String name, int ordinal, Map<String, String> properties) {
    this.name = name;
    this.ordinal = ordinal;
    if (properties != null) {
      this.properties = Collections.unmodifiableMap(properties);
    } else {
      this.properties = Collections.emptyMap();
    }
  }

  protected static Properties getProperties(URL url) {
    try (InputStream is = url.openStream()) {
      return load(is);
    } catch (IOException e) {
      throw new CorantRuntimeException(e,
          "Load properties config source from '%s' occurred an error!", url);
    }
  }

  static Properties load(InputStream is) throws IOException {
    Properties props = new Properties();
    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
    return props;
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}

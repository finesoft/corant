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
import static org.corant.shared.util.Strings.asDefaultString;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.corant.config.CorantConfigSource;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-config
 *
 * Use http://java.sun.com/dtd/properties.dtd
 *
 * @author bingo 上午10:11:23
 *
 */
public class XmlConfigSource extends CorantConfigSource {

  private static final long serialVersionUID = -6510093356770922600L;

  final Map<String, String> properties;

  XmlConfigSource(String name, int ordinal, InputStream is) {
    Properties props = new Properties();
    try {
      props.loadFromXML(is);
      props.replaceAll((k, v) -> asDefaultString(v).replace("\\", "\\\\"));
      properties = Collections.unmodifiableMap(toMap(props));
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  XmlConfigSource(URL resourceUrl, int ordinal) {
    super(shouldNotNull(resourceUrl).toExternalForm(), ordinal);
    Properties props = new Properties();
    try (InputStream is = resourceUrl.openStream()) {
      props.loadFromXML(is);
      props.replaceAll((k, v) -> asDefaultString(v).replace("\\", "\\\\"));
      properties = Collections.unmodifiableMap(toMap(props));
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

}

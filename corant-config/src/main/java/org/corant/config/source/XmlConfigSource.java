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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;

/**
 * corant-config
 * <p>
 * Use <a href="http://java.sun.com/dtd/properties.dtd">pr</a>
 *
 * @author bingo 上午10:11:23
 */
public class XmlConfigSource extends AbstractCorantConfigSource {

  private static final long serialVersionUID = -6510093356770922600L;

  final Map<String, String> properties;

  XmlConfigSource(Resource resource, int ordinal) {
    super(shouldNotNull(resource).getLocation(), ordinal);
    Properties props = new Properties();
    try (InputStream is = resource.openInputStream()) {
      props.loadFromXML(is);
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

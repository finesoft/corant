/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.security.shared;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import org.corant.modules.security.Principal;
import org.corant.shared.exception.NotSupportedException;

public class SimplePrincipal implements Principal, Serializable {

  private static final long serialVersionUID = 282297555381317944L;

  protected String name;
  protected Map<String, ? extends Serializable> properties = Collections.emptyMap();

  public SimplePrincipal(String name, Map<String, ? extends Serializable> properties) {
    this.name = name;
    if (properties != null) {
      this.properties = Collections.unmodifiableMap(properties);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  public Map<String, ? extends Serializable> getProperties() {
    return properties;
  }

  public <T> T getProperty(String name, Class<T> type) {
    Object property;
    if (properties != null && (property = properties.get(name)) != null) {
      return toObject(property, type);
    }
    return null;
  }

  public <T> T getProperty(String name, Class<T> type, T alt) {
    Object property;
    if (properties != null && (property = properties.get(name)) != null) {
      return defaultObject(toObject(property, type), alt);
    }
    return null;
  }

  @Override
  public String toString() {
    return "SimplePrincipal [name=" + name + ", properties=" + properties + "]";
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> cls) {
    if (Principal.class.isAssignableFrom(cls)) {
      return (T) this;
    }
    if (SimplePrincipal.class.isAssignableFrom(cls)) {
      return (T) this;
    }
    throw new NotSupportedException("Can't unwrap %s", cls);
  }
}

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
import static org.corant.shared.util.Maps.getMapCollection;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.IntFunction;
import org.corant.modules.security.Principal;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:17
 *
 */
public class SimplePrincipal implements Principal, Serializable {

  private static final long serialVersionUID = 282297555381317944L;

  protected String name;
  protected Map<String, ? extends Serializable> properties = Collections.emptyMap();

  public SimplePrincipal(String name) {
    this(name, null);
  }

  public SimplePrincipal(String name, Map<String, ? extends Serializable> properties) {
    this.name = name;
    if (properties != null) {
      this.properties = Collections.unmodifiableMap(properties);
    }
  }

  protected SimplePrincipal() {}

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
    return defaultObject(getProperty(name, type), alt);
  }

  public <T, C extends Collection<T>> C getProperty(String name, IntFunction<C> collectionFactory,
      Class<T> itemType) {
    return getMapCollection(properties, name, collectionFactory, itemType, null);
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

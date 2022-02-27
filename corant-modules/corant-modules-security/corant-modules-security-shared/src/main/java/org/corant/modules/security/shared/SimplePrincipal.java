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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.corant.modules.security.Principal;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:17
 *
 */
public class SimplePrincipal implements Principal, AttributeSet, Serializable {

  private static final long serialVersionUID = 282297555381317944L;

  protected String name;

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public SimplePrincipal(String name) {
    this(name, null);
  }

  public SimplePrincipal(String name, Map<String, ? extends Serializable> attributes) {
    this.name = name;
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected SimplePrincipal() {}

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SimplePrincipal other = (SimplePrincipal) obj;
    return Objects.equals(attributes, other.attributes) && Objects.equals(name, other.name);
  }

  @Override
  public Map<String, ? extends Serializable> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, name);
  }

  @Override
  public String toString() {
    return "SimplePrincipal [name=" + name + ", attributes=" + attributes + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (SimplePrincipal.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Principal.super.unwrap(cls);
  }
}

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
import java.util.Map;
import java.util.Objects;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:41:08
 *
 */
public class IdentifierPrincipal extends SimplePrincipal {

  private static final long serialVersionUID = -6975094126652298173L;

  protected Serializable id;

  public IdentifierPrincipal(Serializable id, String name) {
    super(name);
    this.id = id;
  }

  public IdentifierPrincipal(Serializable id, String name,
      Map<String, ? extends Serializable> properties) {
    super(name, properties);
    this.id = id;
  }

  protected IdentifierPrincipal() {}

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    IdentifierPrincipal other = (IdentifierPrincipal) obj;
    return Objects.equals(id, other.id);
  }

  public Serializable getId() {
    return id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    return prime * result + Objects.hash(id);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (IdentifierPrincipal.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return super.unwrap(cls);
  }

}

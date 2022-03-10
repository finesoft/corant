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
import org.corant.modules.security.Permission;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:41:08
 *
 */
public class IdentifiablePermission extends SimplePermission {

  private static final long serialVersionUID = 4147350908179493589L;

  protected Serializable id;

  public IdentifiablePermission(Serializable id, String name) {
    this(id, name, null);
  }

  public IdentifiablePermission(Serializable id, String name,
      Map<String, ? extends Serializable> attributes) {
    this.id = id;
    this.name = name;
    predicate = predicateOf(id);
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected IdentifiablePermission() {}

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
    IdentifiablePermission other = (IdentifiablePermission) obj;
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
  public boolean implies(Permission permission) {
    if (!(permission instanceof IdentifiablePermission)) {
      return false;
    }
    return predicate.test(((IdentifiablePermission) permission).id);
  }

  @Override
  public String toString() {
    return "IdentifiablePermission [id=" + id + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (IdentifiablePermission.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return super.unwrap(cls);
  }

}

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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.areEqual;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.corant.modules.security.Permission;
import org.corant.modules.security.Principal;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:33
 *
 */
public class IdentifiableSubject extends SimpleSubject {

  private static final long serialVersionUID = 3435651508945136478L;

  protected Serializable id;

  public IdentifiableSubject(Serializable id, Collection<? extends Principal> principals) {
    super(principals);
    this.id = shouldNotNull(id);
  }

  public IdentifiableSubject(Serializable id, Collection<? extends Principal> principals,
      Collection<? extends Role> roles, Collection<? extends Permission> permissions,
      Map<String, ? extends Serializable> attributes) {
    super(principals, roles, permissions, attributes);
    this.id = shouldNotNull(id);
  }

  public IdentifiableSubject(Serializable id, Collection<? extends Principal> principals,
      Collection<? extends Role> roles, Map<String, ? extends Serializable> attributes) {
    super(principals, roles, attributes);
    this.id = shouldNotNull(id);
  }

  protected IdentifiableSubject() {}

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
    IdentifiableSubject other = (IdentifiableSubject) obj;
    return Objects.equals(id, other.id);
  }

  @Override
  public Map<String, ? extends Serializable> getAttributes() {
    return attributes;
  }

  public Serializable getId() {
    return id;
  }

  @Override
  public Principal getPrincipal(String name) {
    return principals.stream().filter(p -> areEqual(name, p.getName())).findFirst().orElse(null);
  }

  @Override
  public Collection<? extends Principal> getPrincipals() {
    return principals;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "IdentifiableSubject [id=" + id + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (IdentifiableSubject.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return super.unwrap(cls);
  }
}

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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:41:08
 *
 */
public class IdentifiableRole extends Predication implements Role, AttributeSet {

  private static final long serialVersionUID = 3771872966822697648L;

  protected Serializable id;

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public IdentifiableRole(Serializable id) {
    this(id, null);
  }

  public IdentifiableRole(Serializable id, Map<String, ? extends Serializable> attributes) {
    super(shouldNotNull(id));
    this.id = id;
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected IdentifiableRole() {}

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
    IdentifiableRole other = (IdentifiableRole) obj;
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
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean implies(Role role) {
    if (!(role instanceof IdentifiableRole)) {
      return false;
    }
    return test(((IdentifiableRole) role).id);
  }

  @Override
  public String toString() {
    return "IdentifiableRole [id=" + id + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (IdentifiableRole.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Role.super.unwrap(cls);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    predicate = predicateOf(id);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}

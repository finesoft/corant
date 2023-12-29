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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.corant.modules.security.Permission;
import org.corant.shared.ubiquity.AttributeSet;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:33:46
 */
public class SimplePermission extends Predication implements Permission, AttributeSet {

  private static final long serialVersionUID = 3701989330265355350L;

  protected String name;

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public SimplePermission(String name) {
    this(name, null);
  }

  public SimplePermission(String name, Map<String, ? extends Serializable> attributes) {
    super(shouldNotBlank(name, "The name of simple permission can't blank!"));
    this.name = name;
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected SimplePermission() {}

  public static SimplePermission of(String name) {
    return new SimplePermission(name);
  }

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
    SimplePermission other = (SimplePermission) obj;
    return Objects.equals(attributes, other.attributes) && Objects.equals(name, other.name);
  }

  @Override
  public Map<String, ? extends Serializable> getAttributes() {
    return attributes;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, name);
  }

  @Override
  public boolean implies(Permission permission) {
    if (!(permission instanceof SimplePermission)) {
      return false;
    }
    return test(((SimplePermission) permission).name);
  }

  @Override
  public String toString() {
    return "SimplePermission [name=" + name + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (SimplePermission.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Permission.super.unwrap(cls);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    predicate = predicateOf(name);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

}

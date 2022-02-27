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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.corant.modules.security.Role;
import org.corant.modules.security.shared.util.StringPredicates;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:33:46
 *
 */
public class SimpleRole implements Role, AttributeSet {

  private static final long serialVersionUID = 1585708942349545935L;

  protected String name;

  protected transient Predicate<String> predicate;

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public SimpleRole(String name) {
    this(name, null);
  }

  public SimpleRole(String name, Map<String, ? extends Serializable> attributes) {
    this.name = name;
    predicate = StringPredicates.predicateOf(name);
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected SimpleRole() {}

  public static SimpleRole of(String name) {
    return new SimpleRole(name);
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
    SimpleRole other = (SimpleRole) obj;
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
  public boolean implies(Role role) {
    if (!(role instanceof SimpleRole)) {
      return false;
    }
    return predicate.test(((SimpleRole) role).name);
  }

  @Override
  public String toString() {
    return "SimpleRole [name=" + name + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (SimpleRole.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Role.super.unwrap(cls);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    predicate = StringPredicates.predicateOf(name);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}

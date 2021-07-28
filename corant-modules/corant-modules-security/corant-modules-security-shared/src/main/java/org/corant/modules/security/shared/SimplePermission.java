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

import java.util.function.Predicate;
import org.corant.modules.security.Permission;
import org.corant.modules.security.shared.util.StringPredicates;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:33:46
 *
 */
public class SimplePermission implements Permission {

  private static final long serialVersionUID = 3701989330265355350L;

  private final String name;
  private final Predicate<String> predicate;

  public SimplePermission(String name) {
    this.name = name;
    predicate = StringPredicates.predicateOf(name);
  }

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
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean implies(Permission permission) {
    if (!(permission instanceof SimplePermission)) {
      return false;
    }
    return predicate.test(((SimplePermission) permission).name);
  }

  @Override
  public String toString() {
    return "SimplePermission [name=" + name + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    return null;
  }

}

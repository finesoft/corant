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

import static java.util.Collections.emptyList;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.corant.shared.util.Empties;
import org.corant.shared.util.Objects;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:11
 */
public class SimplePermissions implements Iterable<SimplePermission>, Serializable {

  private static final long serialVersionUID = -7538549347440416322L;

  public static final SimplePermissions EMPTY_INST = new SimplePermissions(null);

  protected final Collection<SimplePermission> perms;

  public SimplePermissions(Collection<? extends SimplePermission> perms) {
    this.perms = Empties.isEmpty(perms) ? emptyList()
        : perms.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableList());
  }

  public static SimplePermissions of(Collection<String> names) {
    if (names != null) {
      return new SimplePermissions(
          names.stream().map(SimplePermission::new).collect(Collectors.toList()));
    }
    return EMPTY_INST;
  }

  public static SimplePermissions of(String... name) {
    if (name.length == 0) {
      return EMPTY_INST;
    }
    return new SimplePermissions(
        Arrays.stream(name).map(SimplePermission::new).collect(Collectors.toList()));
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
    SimplePermissions other = (SimplePermissions) obj;
    return java.util.Objects.equals(perms, other.perms);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(perms);
  }

  public boolean isEmpty() {
    return perms.isEmpty();
  }

  @Override
  public Iterator<SimplePermission> iterator() {
    return perms.iterator();
  }

  public Stream<SimplePermission> stream() {
    return perms.stream();
  }
}

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

import static org.corant.shared.util.Lists.listOf;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:11
 *
 */
public class SimplePermissions implements Iterable<SimplePermission>, Serializable {

  private static final long serialVersionUID = -7538549347440416322L;

  protected final Collection<SimplePermission> perms;

  public SimplePermissions(Collection<SimplePermission> perms) {
    this.perms = perms == null ? Collections.emptyList()
        : perms.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableList());
  }

  public static SimplePermissions of(Collection<String> names) {
    if (names != null) {
      return new SimplePermissions(names.stream().filter(Strings::isNotBlank)
          .map(SimplePermission::new).collect(Collectors.toList()));
    }
    return new SimplePermissions((Collection<SimplePermission>) null);
  }

  public static SimplePermissions of(SimplePermission... perms) {
    return new SimplePermissions(listOf(perms));
  }

  public static SimplePermissions of(String... name) {
    return new SimplePermissions(
        Arrays.stream(name).map(SimplePermission::new).collect(Collectors.toList()));
  }

  @Override
  public Iterator<SimplePermission> iterator() {
    return perms.iterator();
  }

  public Stream<SimplePermission> stream() {
    return perms.stream();
  }

  public List<SimplePermission> toList() {
    return listOf(this);
  }

}

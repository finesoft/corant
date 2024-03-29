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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.corant.shared.util.Empties;
import org.corant.shared.util.Objects;

/**
 *
 * corant-modules-security-shared
 *
 * @author bingo 下午4:22:28
 *
 */
public class SimpleRoles implements Iterable<SimpleRole>, Serializable {

  private static final long serialVersionUID = -7033501395362107655L;

  public static final SimpleRoles EMPTY_INST = new SimpleRoles(null);

  protected final Collection<SimpleRole> roles;

  public SimpleRoles(Collection<? extends SimpleRole> roles) {
    this.roles = Empties.isEmpty(roles) ? Collections.emptyList()
        : roles.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableList());
  }

  public static SimpleRoles of(Collection<String> names) {
    if (names != null) {
      return new SimpleRoles(names.stream().map(SimpleRole::new).collect(Collectors.toList()));
    }
    return EMPTY_INST;
  }

  public static SimpleRoles of(String... name) {
    if (name.length == 0) {
      return EMPTY_INST;
    }
    return new SimpleRoles(Arrays.stream(name).map(SimpleRole::new).collect(Collectors.toList()));
  }

  public boolean isEmpty() {
    return roles.isEmpty();
  }

  @Override
  public Iterator<SimpleRole> iterator() {
    return roles.iterator();
  }

  public Stream<SimpleRole> stream() {
    return roles.stream();
  }
}

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
 * @author bingo 下午4:22:22
 *
 */
public class SimplePrincipals implements Iterable<SimplePrincipal>, Serializable {

  private static final long serialVersionUID = -5640320901365165346L;

  protected final Collection<SimplePrincipal> principals;

  public SimplePrincipals(Collection<? extends SimplePrincipal> principals) {
    this.principals = Empties.isEmpty(principals) ? emptyList()
        : principals.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableList());
  }

  public static SimplePrincipals of(Collection<String> names) {
    if (names != null) {
      return new SimplePrincipals(
          names.stream().map(SimplePrincipal::new).collect(Collectors.toList()));
    }
    return new SimplePrincipals(null);
  }

  public static SimplePrincipals of(String... name) {
    return new SimplePrincipals(
        Arrays.stream(name).map(SimplePrincipal::new).collect(Collectors.toList()));
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
    SimplePrincipals other = (SimplePrincipals) obj;
    return java.util.Objects.equals(principals, other.principals);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(principals);
  }

  public boolean isEmpty() {
    return principals.isEmpty();
  }

  @Override
  public Iterator<SimplePrincipal> iterator() {
    return principals.iterator();
  }

  public Stream<SimplePrincipal> stream() {
    return principals.stream();
  }

}

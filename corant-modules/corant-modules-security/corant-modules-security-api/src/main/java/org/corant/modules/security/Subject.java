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
package org.corant.modules.security;

import static java.util.Collections.emptySet;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Sets.newHashSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午7:19:52
 */
public interface Subject extends Serializable {

  Subject EMPTY_INST = new Subject() {

    private static final long serialVersionUID = 4617421960094030872L;

    @Override
    public Principal getPrincipal(String name) {
      return null;
    }

    @Override
    public Collection<? extends Principal> getPrincipals() {
      return Collections.emptySet();
    }
  };

  default javax.security.auth.Subject asSubject() {
    return new javax.security.auth.Subject(true, newHashSet(getPrincipals()), null, null);
  }

  default <T> T getPrincipal(Class<T> clazz) {
    Collection<? extends Principal> principals;
    if (clazz == null || isEmpty(principals = getPrincipals())) {
      return null;
    }
    return principals.stream().filter(p -> clazz.isAssignableFrom(getUserClass(p)))
        .map(p -> p.unwrap(clazz)).findFirst().orElse(null);
  }

  Principal getPrincipal(String name);

  Collection<? extends Principal> getPrincipals();

  default <T> Collection<T> getPrincipals(Class<T> clazz) {
    Collection<? extends Principal> principals;
    if (clazz == null || isEmpty(principals = getPrincipals())) {
      return emptySet();
    }
    return principals.stream().filter(p -> clazz.isAssignableFrom(getUserClass(p)))
        .map(p -> p.unwrap(clazz)).collect(Collectors.toUnmodifiableSet());
  }

  default <T> T unwrap(Class<T> cls) {
    if (Subject.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap subject to " + cls);
  }

}

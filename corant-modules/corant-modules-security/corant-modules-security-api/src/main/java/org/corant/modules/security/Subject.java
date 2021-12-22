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

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午7:19:52
 *
 */
public interface Subject extends Serializable {

  Serializable getId();

  Principal getPrincipal(String name);

  default <T> T getPrincipal(String name, Class<T> c) {
    Principal p = getPrincipal(name);
    return p != null ? p.unwrap(c) : null;
  }

  Collection<? extends Principal> getPrincipals();

  default <T> Collection<T> getPrincipals(Class<T> c) {
    return getPrincipals().stream().map(p -> p.unwrap(c)).collect(Collectors.toList());
  }

  default <T> T unwrap(Class<T> cls) {
    if (Subject.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap subject to " + cls);
  }

}

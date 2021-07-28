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

import static org.corant.shared.util.Objects.areEqual;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.modules.security.Principal;
import org.corant.modules.security.Subject;
import org.corant.shared.exception.NotSupportedException;

public class SimpleSubject implements Subject {

  private static final long serialVersionUID = 3435651508945136478L;

  private Serializable id;

  private Set<Principal> principals = new LinkedHashSet<>();

  public SimpleSubject(Serializable id, Principal... principals) {
    this.id = id;
    for (Principal principal : principals) {
      this.principals.add(principal);
    }
  }

  @Override
  public Serializable getId() {
    return id;
  }

  @Override
  public Principal getPrincipal(String name) {
    return principals.stream().filter(p -> areEqual(name, p.getName())).findFirst().orElse(null);
  }

  @Override
  public Set<Principal> getPrincipals() {
    return Collections.unmodifiableSet(principals);
  }

  @Override
  public String toString() {
    return "SimpleSubject [id=" + id + "]";
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> cls) {
    if (Subject.class.isAssignableFrom(cls)) {
      return (T) this;
    }
    if (SimpleSubject.class.isAssignableFrom(cls)) {
      return (T) this;
    }
    throw new NotSupportedException("Can't unwrap %s", cls);
  }
}

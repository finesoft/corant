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
package org.corant.context.security;

import static java.util.Collections.emptySet;
import java.io.Serializable;
import java.util.Set;

/**
 * corant-context
 *
 * <p>
 * Implementers are recommended to implement this interface as immutable objects.
 *
 * @author bingo 下午5:20:40
 */
public interface SecurityContext extends Serializable {

  SecurityContext EMPTY_INST = new SecurityContext() {

    private static final long serialVersionUID = -2636803453765182930L;

    @Override
    public String getAuthenticationScheme() {
      return null;
    }

    @Override
    public Serializable getCallerPrincipal() {
      return null;
    }

    @Override
    public <T extends Serializable> T getPrincipal(Class<T> cls) {
      return null;
    }

    @Override
    public <T extends Serializable> Set<T> getPrincipals(Class<T> cls) {
      return emptySet();
    }

  };

  String getAuthenticationScheme();

  /**
   * Retrieve the platform-specific principal that represents the name of authenticated caller, or
   * null if the current caller is not authenticated.
   *
   * @return Serializable representing the name of the current authenticated user, or null if not
   *         authenticated.
   */
  Serializable getCallerPrincipal();

  /**
   * Retrieve first one principal of the given type from the authenticated caller's Subject, or null
   * if the current caller is not authenticated, or if the specified type isn't found in the
   * Subject.
   *
   * @param <T> the principal type
   * @param cls Class object representing the type of Principal to return.
   * @return principal of the given type, or null.
   */
  <T extends Serializable> T getPrincipal(Class<T> cls);

  /**
   * Retrieve all Principals of the given type from the authenticated caller's Subject, or an empty
   * set if the current caller is not authenticated, or if the specified type isn't found in the
   * Subject.
   * <p>
   * This can be used to retrieve application-specific Principals when the platform's representation
   * of the caller uses a different principal type.
   * <p>
   * The returned Set is not backed by the Subject's internal Principal Set. A new Set is created
   * and returned for each method invocation. Modifications to the returned Set will not affect the
   * internal Principal Set.
   *
   * @param <T> the principal type
   * @param cls Class object representing the type of Principal to return.
   *
   * @return Set of Principals of the given type, or an empty set.
   */
  <T extends Serializable> Set<T> getPrincipals(Class<T> cls);

  default <T> T unwrap(Class<T> cls) {
    if (SecurityContext.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap security context to " + cls);
  }

}

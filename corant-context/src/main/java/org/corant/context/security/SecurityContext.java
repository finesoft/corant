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

import java.io.Serializable;

/**
 * corant-context
 *
 * <p>
 * Implementers are recommended to implement this interface as immutable objects.
 *
 * @author bingo 下午5:20:40
 *
 */
public interface SecurityContext extends Serializable {

  SecurityContext EMPTY_INST = new SecurityContext() {

    private static final long serialVersionUID = -2636803453765182930L;

    @Override
    public String getAuthenticationScheme() {
      return null;
    }

    @Override
    public Serializable getPrincipal() {
      return null;
    }

    @Override
    public <T> T getPrincipal(Class<T> cls) {
      return null;
    }

  };

  String getAuthenticationScheme();

  Serializable getPrincipal();

  <T> T getPrincipal(Class<T> cls);

  default <T> T unwrap(Class<T> cls) {
    if (SecurityContext.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap security context to " + cls);
  }

}

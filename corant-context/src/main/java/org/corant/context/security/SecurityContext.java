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

  DefaultSecurityContext EMPTY_INST = new DefaultSecurityContext(null, null, null);

  String getAuthenticationScheme();

  Principal getPrincipal();

  Subject getSubject();

  class DefaultSecurityContext implements SecurityContext {

    private static final long serialVersionUID = 4329263253208902621L;

    protected final String authenticationScheme;
    protected final Principal principal;
    protected final Subject subject;

    public DefaultSecurityContext(String authenticationScheme, Subject subject,
        Principal principal) {
      this.authenticationScheme = authenticationScheme;
      this.principal = principal;
      this.subject = subject;
    }

    @Override
    public String getAuthenticationScheme() {
      return authenticationScheme;
    }

    @Override
    public Principal getPrincipal() {
      return principal;
    }

    @Override
    public Subject getSubject() {
      return subject;
    }

    @Override
    public String toString() {
      return "DefaultSecurityContext [authenticationScheme=" + authenticationScheme + ", principal="
          + principal + ", subject=" + subject + "]";
    }

  }
}

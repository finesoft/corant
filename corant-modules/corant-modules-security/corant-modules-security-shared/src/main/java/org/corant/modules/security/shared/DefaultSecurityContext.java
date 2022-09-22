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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Sets.immutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.context.security.SecurityContext;
import org.corant.modules.security.Principal;
import org.corant.modules.security.Subject;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:14:35
 *
 */
public class DefaultSecurityContext implements SecurityContext {

  private static final long serialVersionUID = 4329263253208902621L;

  protected final String authenticationScheme;

  protected final Collection<? extends Principal> principals;

  public DefaultSecurityContext(String authenticationScheme, Principal principal) {
    this.authenticationScheme = authenticationScheme;
    principals = principal == null ? emptySet() : singleton(principal);
  }

  public DefaultSecurityContext(String authenticationScheme, Subject subject) {
    this.authenticationScheme = authenticationScheme;
    principals = subject == null ? emptySet() : immutableSet(subject.getPrincipals());
  }

  @Override
  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  @Override
  public Principal getCallerPrincipal() {
    return isEmpty(principals) ? null : principals.iterator().next();
  }

  @Override
  public <T extends Serializable> T getPrincipal(Class<T> cls) {
    return cls == null || isEmpty(principals) ? null
        : principals.stream().filter(p -> cls.isAssignableFrom(getUserClass(p)))
            .map(p -> p.unwrap(cls)).findFirst().orElse(null);
  }

  @Override
  public <T extends Serializable> Set<T> getPrincipals(Class<T> cls) {
    return cls == null ? emptySet()
        : principals.stream().filter(p -> cls.isAssignableFrom(getUserClass(p)))
            .map(p -> p.unwrap(cls)).collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String toString() {
    return "DefaultSecurityContext [authenticationScheme=" + authenticationScheme + ", principals="
        + principals + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (DefaultSecurityContext.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return SecurityContext.super.unwrap(cls);
  }

}

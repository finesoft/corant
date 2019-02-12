/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.asosat.ddd.security;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.SecurityContext;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.security.shared.SecurityContextHolder;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:23:41
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultSecurityContextHolder implements SecurityContextHolder {

  static final ThreadLocal<DefaultSecurityContext> securityContextHolder = new ThreadLocal<>();

  @Override
  public DefaultSecurityContext get() {
    return securityContextHolder.get();
  }

  public Participator getCurrentOrg() {
    if (securityContextHolder.get() != null) {
      return securityContextHolder.get().getOrgPrincipal();
    }
    return Participator.empty();
  }

  public Participator getCurrentUser() {
    if (securityContextHolder.get() != null) {
      return securityContextHolder.get().getUserPrincipal();
    }
    return Participator.empty();
  }

  @Override
  public DefaultSecurityContext put(SecurityContext securityContext) {
    if (securityContext instanceof DefaultSecurityContext) {
      DefaultSecurityContext sc = DefaultSecurityContext.class.cast(securityContext);
      securityContextHolder.set(sc);
      return sc;
    }
    return null;
  }

  @Override
  public void remove() {
    securityContextHolder.remove();
  }

}

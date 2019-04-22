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

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import org.corant.asosat.ddd.domain.shared.Participator;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.security.shared.SecurityContextHolder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:23:41
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultSecurityContextHolder implements SecurityContextHolder {

  static final ThreadLocal<DefaultSecurityContext> holder = new ThreadLocal<>();

  @Inject
  @ConfigProperty(name = "security.enable-default-context", defaultValue = "false")
  Boolean enableDefaultContext;

  @Inject
  @ConfigProperty(name = "security.default-context-org-id")
  Optional<String> defaultOrgId;

  @Inject
  @ConfigProperty(name = "security.default-context-org-name")
  Optional<String> defaultOrgName;

  @Inject
  @ConfigProperty(name = "security.default-context-user-id")
  Optional<String> defaultUserId;

  @Inject
  @ConfigProperty(name = "security.default-context-user-name")
  Optional<String> defaultUserName;

  @Inject
  @ConfigProperty(name = "security.default-access-token")
  Optional<String> defaultAccessToken;

  @Inject
  @ConfigProperty(name = "security.default-refresh-token")
  Optional<String> defaultRefreshToken;

  @Inject
  @ConfigProperty(name = "security.default-authentication-scheme")
  Optional<String> defaultAuthenticationScheme;

  @Inject
  @ConfigProperty(name = "security.default-secure", defaultValue = "false")
  Boolean defaultSecure;

  @Inject
  @ConfigProperty(name = "security.default-user-roles", defaultValue = "false")
  Optional<Set<String>> defaultUserRoles;

  volatile DefaultSecurityContext defaultContxt;

  @Override
  public DefaultSecurityContext get() {
    return enableDefaultContext ? defaultObject(holder.get(), defaultContxt) : holder.get();
  }

  public Participator getCurrentOrg() {
    if (holder.get() != null) {
      return holder.get().getOrgPrincipal();
    } else if (enableDefaultContext && defaultContxt != null) {
      return defaultContxt.getOrgPrincipal();
    }
    return Participator.empty();
  }

  public Participator getCurrentUser() {
    if (holder.get() != null) {
      return holder.get().getUserPrincipal();
    } else if (enableDefaultContext && defaultContxt != null) {
      return defaultContxt.getUserPrincipal();
    }
    return Participator.empty();
  }

  @Override
  public DefaultSecurityContext put(SecurityContext securityContext) {
    if (securityContext instanceof DefaultSecurityContext) {
      DefaultSecurityContext sc = DefaultSecurityContext.class.cast(securityContext);
      holder.set(sc);
      return sc;
    }
    return null;
  }

  @Override
  public void remove() {
    holder.remove();
  }

  @PostConstruct
  void onPostConstruct() {
    if (enableDefaultContext) {
      defaultContxt = new DefaultSecurityContext(defaultAccessToken.orElse(null),
          defaultRefreshToken.orElse(null),
          new Participator(defaultUserId.orElse(null), defaultUserName.orElse(null)),
          new Participator(defaultOrgId.orElse(null), defaultOrgName.orElse(null)), defaultSecure,
          defaultAuthenticationScheme.orElse(null),
          defaultUserRoles.orElse(Collections.emptySet()));
    }
  }
}

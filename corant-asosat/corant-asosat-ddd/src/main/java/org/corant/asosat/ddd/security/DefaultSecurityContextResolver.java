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

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.security.keycloak.KeycloakJaxrsSecurityContextResolver;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:31:22
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultSecurityContextResolver implements KeycloakJaxrsSecurityContextResolver {

  @Override
  public DefaultSecurityContext resolve(
      KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, boolean isSecure,
      String authenticationScheme, Set<String> roles) {
    return null;
  }

}

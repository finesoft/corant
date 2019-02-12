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
package org.corant.suites.security.keycloak;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.corant.suites.security.shared.SecurityRequestUrlMatcher;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;
import org.keycloak.jaxrs.JaxrsHttpFacade;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午7:40:34
 *
 */
@ApplicationScoped
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Provider
public class KeycloakJaxrsBearerTokenFilter extends JaxrsBearerTokenFilterImpl {

  public static final String AUTH_SCHEMA = "OAUTH_BEARER";

  @Inject
  Logger logger;

  @Inject
  SecurityRequestUrlMatcher urlMatcher;

  @Inject
  @Any
  Instance<KeycloakJaxrsSecurityContextResolver> jscResolver;

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    if (urlMatcher.isCoveredUrl(request.getUriInfo().getBaseUri().getPath())) {
      super.filter(request);
    }
  }

  @Override
  protected void propagateSecurityContext(JaxrsHttpFacade facade, ContainerRequestContext request,
      KeycloakDeployment resolvedDeployment, BearerTokenRequestAuthenticator bearer) {
    RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(
        resolvedDeployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);

    // Not needed to do resteasy specifics as KeycloakSecurityContext can be always retrieved from
    // SecurityContext by typecast SecurityContext.getUserPrincipal to KeycloakPrincipal
    // ResteasyProviderFactory.pushContext(KeycloakSecurityContext.class, skSession);

    facade.setSecurityContext(skSession);
    String principalName = AdapterUtils.getPrincipalName(resolvedDeployment, bearer.getToken());
    KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal =
        new KeycloakPrincipal<>(principalName, skSession);
    SecurityContext anonymousSecurityContext = getRequestSecurityContext(request);
    boolean isSecure = anonymousSecurityContext.isSecure();
    Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
    request.setSecurityContext(resolveSecurityContext(principal, isSecure, roles));
  }

  protected SecurityContext resolveSecurityContext(
      KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, boolean isSecure,
      Set<String> roles) {
    if (jscResolver.isResolvable()) {
      return jscResolver.get().resolve(principal, isSecure, AUTH_SCHEMA, roles);
    } else {
      return new SecurityContext() {
        @Override
        public String getAuthenticationScheme() {
          return AUTH_SCHEMA;
        }

        @Override
        public Principal getUserPrincipal() {
          return principal;
        }

        @Override
        public boolean isSecure() {
          return isSecure;
        }

        @Override
        public boolean isUserInRole(String role) {
          return roles.contains(role);
        }
      };
    }
  }

}

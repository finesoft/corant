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
import javax.annotation.PostConstruct;
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
import org.corant.shared.util.Resources.SourceType;
import org.corant.suites.security.shared.SecurityContextHolder;
import org.corant.suites.security.shared.SecurityRequestUrlMatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

  @Inject
  @Any
  Instance<SecurityContextHolder> jscHolder;

  @Inject
  @ConfigProperty(name = "security.keycloak.config-file-path",
      defaultValue = "META-INF/keycloak.json")
  String configFilePath;

  @Inject
  @ConfigProperty(name = "security.keycloak.enable", defaultValue = "true")
  boolean enabled;

  @Override
  public void filter(ContainerRequestContext request) throws IOException {
    if (jscHolder.isResolvable()) {
      if (urlMatcher.isCoveredUrl(request.getUriInfo().getBaseUri().getPath()) && isEnabled()) {
        super.filter(request);
      }
    } else {
      logger.warning(
          () -> "The keycloak filter not available yet, can not find SecurityContextHolder instance for inject!");
    }
  }

  public boolean isEnabled() {
    return enabled;
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
    SecurityContext sc = resolveSecurityContext(principal, isSecure, roles);
    request.setSecurityContext(jscHolder.get().put(sc));
  }

  protected SecurityContext resolveSecurityContext(
      KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, boolean isSecure,
      Set<String> roles) {
    if (jscResolver.isResolvable()) {
      return jscResolver.get().resolve(principal, isSecure, AUTH_SCHEMA, roles);
    } else {
      return new KeycloakSecurityContext(isSecure, principal, roles);
    }
  }

  @PostConstruct
  void onPostConstruct() {
    if (isEnabled()) {
      setKeycloakConfigFile(SourceType.CLASS_PATH.regulate(configFilePath));// FIXME
      logger.info(() -> String.format(
          "Enabled keycloak jaxrs bearer token filter, the keycloak config file is %s",
          configFilePath));
    } else {
      logger.info(() -> "Keycloak jaxrs bearer token filter doesn't enable!");
    }
  }

  /**
   * corant-suites-security-keycloak
   *
   * @author bingo 下午3:01:37
   *
   */
  public static final class KeycloakSecurityContext implements SecurityContext {
    private final boolean isSecure;
    private final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;
    private final Set<String> roles;

    /**
     * @param isSecure
     * @param principal
     * @param roles
     */
    public KeycloakSecurityContext(boolean isSecure,
        KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, Set<String> roles) {
      this.isSecure = isSecure;
      this.principal = principal;
      this.roles = roles;
    }

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
  }
}

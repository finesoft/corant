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
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.corant.suites.security.jaxrs.AbstractJaxrsSecurityRequestFilter;
import org.corant.suites.security.jaxrs.JaxrsSecurityContext;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.BasicAuthRequestAuthenticator;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.QueryParamterTokenRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.UserSessionManagement;

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
public class KeycloakSecurityFilter extends AbstractJaxrsSecurityRequestFilter {

  @Inject
  Logger logger;

  @Inject
  KeycloakConfigResolverImpl kcConfigResolver;

  @Inject
  UserSessionManagement kcUserSessionManagement;

  AdapterDeploymentContext kcDeploymentContext;

  NodesRegistrationManagement kcNodesRegistrationManagement;

  @Override
  public void doSecurityFilter(ContainerRequestContext crc) throws IOException {
    if (!kcConfigResolver.isConfigured()) {
      return;
    }
    SecurityContext sc = crc.getSecurityContext();
    KeycloakOIDCHttpFacade facade = new KeycloakOIDCHttpFacade(crc, sc);
    if (handlePreauth(facade)) {
      return;
    }
    KeycloakDeployment resolvedDeployment = kcDeploymentContext.resolveDeployment(facade);
    kcNodesRegistrationManagement.tryRegister(resolvedDeployment);
    bearerAuthentication(facade, crc, resolvedDeployment);
  }

  @PreDestroy
  void destroy() {
    kcNodesRegistrationManagement.stop();
  }

  @PostConstruct
  void init() {
    if (kcConfigResolver.isConfigured()) {
      kcDeploymentContext = new AdapterDeploymentContext(kcConfigResolver);
      kcNodesRegistrationManagement = new NodesRegistrationManagement();
    }
  }

  private void bearerAuthentication(KeycloakOIDCHttpFacade facade, ContainerRequestContext request,
      KeycloakDeployment deployment) {

    BearerTokenRequestAuthenticator authenticator = new BearerTokenRequestAuthenticator(deployment);
    // Get token from header 'Authorization' that has 'Bearer'
    AuthOutcome outcome = authenticator.authenticate(facade);

    // If token not found we need to try use query parameter
    if (outcome == AuthOutcome.NOT_ATTEMPTED) {
      authenticator = new QueryParamterTokenRequestAuthenticator(deployment);
      outcome = authenticator.authenticate(facade);
    }
    // The token was not found from header and query parameter, and deployment is enable basic auth.
    if (outcome == AuthOutcome.NOT_ATTEMPTED && deployment.isEnableBasicAuth()) {
      authenticator = new BasicAuthRequestAuthenticator(deployment);
      outcome = authenticator.authenticate(facade);
    }
    // We got the result, may be failed or token not found in header(Bearer,Basic)/query parameter
    if (outcome == AuthOutcome.FAILED || outcome == AuthOutcome.NOT_ATTEMPTED) {
      AuthChallenge challenge = authenticator.getChallenge();
      if (!challenge.challenge(facade)) {
        facade.getResponse().setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
      }
      if (!facade.isResponseFinished()) {
        facade.getResponse().end();
      }
      return;
    } else if (verifySslFailed(facade, deployment)) {
      return;
    }
    propagateSecurityContext(facade, request, deployment, authenticator);
    handleAuthActions(facade, deployment);
  }

  private void handleAuthActions(KeycloakOIDCHttpFacade facade, KeycloakDeployment deployment) {
    AuthenticatedActionsHandler authActionsHandler =
        new AuthenticatedActionsHandler(deployment, facade);
    if (authActionsHandler.handledRequest()) {
      if (!facade.isResponseFinished()) {
        facade.getResponse().end();
      }
    }
  }

  private boolean handlePreauth(KeycloakOIDCHttpFacade facade) {
    PreAuthActionsHandler handler =
        new PreAuthActionsHandler(kcUserSessionManagement, kcDeploymentContext, facade);
    if (handler.handleRequest()) {
      if (!facade.isResponseFinished()) {
        facade.getResponse().end();
      }
      return true;
    }

    return false;
  }

  // FIXME inject userId orgId orgName to security context.
  private void propagateSecurityContext(KeycloakOIDCHttpFacade facade,
      ContainerRequestContext request, KeycloakDeployment resolvedDeployment,
      BearerTokenRequestAuthenticator bearer) {
    final RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(
        resolvedDeployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);
    facade.setSecurityContext(skSession);
    final String principalName =
        AdapterUtils.getPrincipalName(resolvedDeployment, bearer.getToken());
    final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal =
        new KeycloakPrincipal<>(principalName, skSession);
    final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
    request.setSecurityContext(new JaxrsSecurityContext(principal, principalName, roles,
        request.getSecurityContext().isSecure()));
  }

  private boolean verifySslFailed(KeycloakOIDCHttpFacade facade, KeycloakDeployment deployment) {
    if (!facade.getRequest().isSecure()
        && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
      logger.warning(() -> "SSL is required to authenticate, but request is not secured");
      facade.getResponse().sendError(Response.Status.FORBIDDEN.getStatusCode(), "SSL required!");
      return true;
    }
    return false;
  }
}

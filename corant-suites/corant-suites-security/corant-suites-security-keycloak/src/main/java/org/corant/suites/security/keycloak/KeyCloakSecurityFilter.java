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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.cert.X509Certificate;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassPaths;
import org.corant.shared.util.ClassPaths.ResourceInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.BasicAuthRequestAuthenticator;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.QueryParamterTokenRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.HttpFacade.Request;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.common.util.HostUtils;

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
public class KeyCloakSecurityFilter implements ContainerRequestFilter {

  @Inject
  Logger logger;

  @Inject
  MyKeycloakConfigResolver kcConfigResolver;

  @Inject
  UserSessionManagement kcUserSessionManagement;

  AdapterDeploymentContext kcDeploymentContext;

  NodesRegistrationManagement kcNodesRegistrationManagement;

  @Override
  public void filter(ContainerRequestContext crc) throws IOException {
    if (!kcConfigResolver.isConfigured()) {
      return;
    }
    SecurityContext sc = crc.getSecurityContext();
    MyJaxrsHttpFacade facade = new MyJaxrsHttpFacade(crc, sc);
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

  private void bearerAuthentication(MyJaxrsHttpFacade facade, ContainerRequestContext request,
      KeycloakDeployment resolvedDeployment) {
    BearerTokenRequestAuthenticator authenticator =
        new BearerTokenRequestAuthenticator(resolvedDeployment);
    AuthOutcome outcome = authenticator.authenticate(facade);

    if (outcome == AuthOutcome.NOT_ATTEMPTED) {
      authenticator = new QueryParamterTokenRequestAuthenticator(resolvedDeployment);
      outcome = authenticator.authenticate(facade);
    }

    if (outcome == AuthOutcome.NOT_ATTEMPTED && resolvedDeployment.isEnableBasicAuth()) {
      authenticator = new BasicAuthRequestAuthenticator(resolvedDeployment);
      outcome = authenticator.authenticate(facade);
    }

    if (outcome == AuthOutcome.FAILED || outcome == AuthOutcome.NOT_ATTEMPTED) {
      AuthChallenge challenge = authenticator.getChallenge();
      boolean challengeSent = challenge.challenge(facade);
      if (!challengeSent) {
        // Use some default status code
        facade.getResponse().setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
      }
      // Send response now (if not already sent)
      if (!facade.isResponseFinished()) {
        facade.getResponse().end();
      }
      return;
    } else {
      if (verifySslFailed(facade, resolvedDeployment)) {
        return;
      }
    }

    propagateSecurityContext(facade, request, resolvedDeployment, authenticator);
    handleAuthActions(facade, resolvedDeployment);
  }

  private void handleAuthActions(MyJaxrsHttpFacade facade, KeycloakDeployment deployment) {
    AuthenticatedActionsHandler authActionsHandler =
        new AuthenticatedActionsHandler(deployment, facade);
    if (authActionsHandler.handledRequest()) {
      if (!facade.isResponseFinished()) {
        facade.getResponse().end();
      }
    }
  }

  private boolean handlePreauth(MyJaxrsHttpFacade facade) {
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

  private void propagateSecurityContext(MyJaxrsHttpFacade facade, ContainerRequestContext request,
      KeycloakDeployment resolvedDeployment, BearerTokenRequestAuthenticator bearer) {
    final RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(
        resolvedDeployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);
    facade.setSecurityContext(skSession);
    final String principalName =
        AdapterUtils.getPrincipalName(resolvedDeployment, bearer.getToken());
    final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal =
        new KeycloakPrincipal<>(principalName, skSession);
    final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
    request.setSecurityContext(
        new MySecurityContext(principal, roles, request.getSecurityContext().isSecure()));
  }

  private boolean verifySslFailed(MyJaxrsHttpFacade facade, KeycloakDeployment deployment) {
    if (!facade.getRequest().isSecure()
        && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
      logger.warning(() -> "SSL is required to authenticate, but request is not secured");
      facade.getResponse().sendError(403, "SSL required!");
      return true;
    }
    return false;
  }

  public static class MyJaxrsHttpFacade implements OIDCHttpFacade {

    protected final ContainerRequestContext requestContext;
    protected final SecurityContext securityContext;
    protected final RequestFacade requestFacade = new RequestFacade();
    protected final ResponseFacade responseFacade = new ResponseFacade();
    protected KeycloakSecurityContext keycloakSecurityContext;
    protected boolean responseFinished;

    MyJaxrsHttpFacade(ContainerRequestContext containerRequestContext,
        SecurityContext securityContext) {
      requestContext = containerRequestContext;
      this.securityContext = securityContext;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
      throw new IllegalStateException("Not supported yet");
    }

    @Override
    public HttpFacade.Request getRequest() {
      return requestFacade;
    }

    @Override
    public HttpFacade.Response getResponse() {
      return responseFacade;
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
      return keycloakSecurityContext;
    }

    boolean isResponseFinished() {
      return responseFinished;
    }

    void setSecurityContext(KeycloakSecurityContext securityContext) {
      keycloakSecurityContext = securityContext;
    }

    protected class RequestFacade implements OIDCHttpFacade.Request {

      @Override
      public HttpFacade.Cookie getCookie(String cookieName) {
        Map<String, javax.ws.rs.core.Cookie> cookies = requestContext.getCookies();
        if (cookies == null) {
          return null;
        }
        javax.ws.rs.core.Cookie cookie = cookies.get(cookieName);
        if (cookie == null) {
          return null;
        }
        return new HttpFacade.Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(),
            cookie.getDomain(), cookie.getPath());
      }

      @Override
      public String getFirstParam(String param) {
        throw new RuntimeException("NOT IMPLEMENTED");
      }

      @Override
      public String getHeader(String name) {
        return requestContext.getHeaderString(name);
      }

      @Override
      public List<String> getHeaders(String name) {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return headers == null ? null : headers.get(name);
      }

      @Override
      public InputStream getInputStream() {
        return requestContext.getEntityStream();
      }

      @Override
      public InputStream getInputStream(boolean buffered) {
        if (!buffered) {
          return getInputStream();
        } else {
          return new BufferedInputStream(getInputStream());
        }
      }

      @Override
      public String getMethod() {
        return requestContext.getMethod();
      }

      @Override
      public String getQueryParamValue(String param) {
        MultivaluedMap<String, String> queryParams =
            requestContext.getUriInfo().getQueryParameters();
        if (queryParams == null) {
          return null;
        }
        return queryParams.getFirst(param);
      }

      @Override
      public String getRelativePath() {
        return requestContext.getUriInfo().getPath();
      }

      @Override
      public String getRemoteAddr() {
        // TODO: implement properly
        return HostUtils.getIpAddress();
      }

      @Override
      public String getURI() {
        return requestContext.getUriInfo().getRequestUri().toString();
      }

      @Override
      public boolean isSecure() {
        return securityContext.isSecure();
      }

      @Override
      public void setError(AuthenticationError error) {
        requestContext.setProperty(AuthenticationError.class.getName(), error);
      }

      @Override
      public void setError(LogoutError error) {
        requestContext.setProperty(LogoutError.class.getName(), error);

      }
    }

    protected class ResponseFacade implements OIDCHttpFacade.Response {

      private javax.ws.rs.core.Response.ResponseBuilder responseBuilder =
          javax.ws.rs.core.Response.status(204);

      @Override
      public void addHeader(String name, String value) {
        responseBuilder.header(name, value);
      }

      @Override
      public void end() {
        javax.ws.rs.core.Response response = responseBuilder.build();
        requestContext.abortWith(response);
        responseFinished = true;
      }

      @Override
      public OutputStream getOutputStream() {
        // For now doesn't need to be supported
        throw new IllegalStateException("Not supported yet");
      }

      @Override
      public void resetCookie(String name, String path) {
        // For now doesn't need to be supported
        throw new IllegalStateException("Not supported yet");
      }

      @Override
      public void sendError(int code) {
        javax.ws.rs.core.Response response = responseBuilder.status(code).build();
        requestContext.abortWith(response);
        responseFinished = true;
      }

      @Override
      public void sendError(int code, String message) {
        javax.ws.rs.core.Response response = responseBuilder.status(code).entity(message).build();
        requestContext.abortWith(response);
        responseFinished = true;
      }

      @Override
      public void setCookie(String name, String value, String path, String domain, int maxAge,
          boolean secure, boolean httpOnly) {
        // For now doesn't need to be supported
        throw new IllegalStateException("Not supported yet");
      }

      @Override
      public void setHeader(String name, String value) {
        responseBuilder.header(name, value);
      }

      @Override
      public void setStatus(int status) {
        responseBuilder.status(status);
      }
    }
  }

  @ApplicationScoped
  public static class MyKeycloakConfigResolver implements KeycloakConfigResolver {

    KeycloakDeployment deployment;

    @Inject
    @ConfigProperty(name = "secutiry.keycloak.deployment-file-path",
        defaultValue = "META-INF/keycloak.json")
    String deploymentFilePath;

    @Override
    public KeycloakDeployment resolve(Request facade) {
      return deployment;
    }

    @PostConstruct
    void init() {
      URL kcf;
      try {
        kcf = ClassPaths.from(deploymentFilePath).getResources().map(ResourceInfo::getUrl)
            .findFirst().orElse(null);
        if (kcf != null) {
          try (InputStream is = kcf.openStream()) {
            deployment = KeycloakDeploymentBuilder.build(is);
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        }
      } catch (IOException e1) {
      }
    }

    boolean isConfigured() {
      return deployment != null && deployment.isConfigured();
    }

  }

  public static class MySecurityContext implements SecurityContext {
    private final Principal principal;
    private final Set<String> roles = new HashSet<>();
    private final boolean secure;

    public MySecurityContext(Principal principal, Set<String> roles, boolean secure) {
      super();
      this.principal = principal;
      if (roles != null) {
        this.roles.addAll(roles);
      }
      this.secure = secure;
    }

    @Override
    public String getAuthenticationScheme() {
      return "OAUTH";
    }

    @Override
    public Principal getUserPrincipal() {
      return principal;
    }

    @Override
    public boolean isSecure() {
      return secure;
    }

    @Override
    public boolean isUserInRole(String role) {
      return false;
    }

  }

  @ApplicationScoped
  public static class MyUserSessionManagement implements UserSessionManagement {

    @Override
    public void logoutAll() {}

    @Override
    public void logoutHttpSessions(List<String> ids) {}

  }
}

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
package org.corant.modules.microprofile.jwt.jaxrs;

import java.io.IOException;
import java.security.Principal;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.annotation.Priority;
import org.corant.config.Configs;
import org.corant.modules.microprofile.jwt.MpJWTAuthenticator;
import org.corant.modules.microprofile.jwt.MpJWTJsonWebToken;
import org.corant.modules.microprofile.jwt.MpJWTPrincipal;
import org.corant.modules.security.AuthenticationData;
import org.corant.modules.security.Authenticator;
import org.corant.modules.security.SecurityContextManager;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午2:37:35
 *
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class MpJWTAuthenticationFilter extends JWTAuthenticationFilter {

  public static final String AUTHC_EXCEPTION_KEY = "___AUTHC-EX___";
  public static final String AUTHZ_EXCEPTION_KEY = "___AUTHZ-EX___";
  public static final boolean SUPPRESS_UNAUTHENTICATED =
      Configs.getValue("corant.microprofile.jwt.suppress-unauthenticated", Boolean.class, false);

  protected static final Logger logger = Logger.getLogger(MpJWTAuthenticationFilter.class);
  protected static final boolean debugLogging = logger.isDebugEnabled();

  @Inject
  protected JWTAuthContextInfo authContextInfo;

  @Inject
  protected PrincipalProducer producer;

  @Inject
  @Any
  protected Instance<SecurityContextManager<ContainerRequestContext>> securityManagers;

  @Inject
  @Any
  protected Instance<Authenticator> authenticatorInstance;

  protected Authenticator authenticator = MpJWTAuthenticator.DFLT_INST;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    final SecurityContext securityContext = requestContext.getSecurityContext();
    final Principal principal = securityContext.getUserPrincipal();
    if (!(principal instanceof JsonWebToken)) {
      AbstractBearerTokenExtractor extractor =
          new BearerTokenExtractor(requestContext, authContextInfo);
      String bearerToken = extractor.getBearerToken();
      if (bearerToken != null) {
        try {
          AuthenticationData authcData =
              authenticator.authenticate(new MpJWTJsonWebToken(bearerToken, requestContext));
          JsonWebToken jwtPrincipal = authcData.getPrincipal(MpJWTPrincipal.class);
          producer.setJsonWebToken(jwtPrincipal);
          // Install the JWT principal as the caller
          JWTSecurityContext jwtSctx = new JWTSecurityContext(securityContext, jwtPrincipal);
          requestContext.setSecurityContext(jwtSctx);
          if (securityManagers.isResolvable()) {
            securityManagers.get().bind(requestContext);
          }
          logger.debugf("JWT authentication filter handle successfully");
        } catch (Exception e) {
          if (SUPPRESS_UNAUTHENTICATED) {
            if (debugLogging) {
              logger.warnf(e, "Unable to parse/validate JWT: %s.", e.getMessage());
            } else {
              logger.warnf("Unable to parse/validate JWT: %s.", e.getMessage());
            }
            requestContext.setProperty(AUTHC_EXCEPTION_KEY, e);
          } else {
            // A server using HTTP authentication will respond with a 401 Unauthorized response to a
            // request for a protected resource. This response must include at least one
            // WWW-Authenticate header and at least one challenge, to indicate what authentication
            // schemes can be used to access the resource (and any additional data that each
            // particular scheme needs). See also
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication
            throw new NotAuthorizedException(e, "Bearer");
          }
        }
      }
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    if (authenticatorInstance.isResolvable()) {
      authenticator = authenticatorInstance.get();
    } else if (!authenticatorInstance.isUnsatisfied()) {
      authenticator = authenticatorInstance.stream().sorted(Sortable::compare).findFirst().get();
    }
  }

  /**
   * A delegating JAX-RS SecurityContext prototype that provides access to the JWTCallerPrincipal
   * TODO
   */
  public static class JWTSecurityContext implements SecurityContext {
    private SecurityContext delegate;
    private JsonWebToken principal;

    JWTSecurityContext(SecurityContext delegate, JsonWebToken principal) {
      this.delegate = delegate;
      this.principal = principal;
    }

    @Override
    public String getAuthenticationScheme() {
      return delegate.getAuthenticationScheme();
    }

    @Override
    public Principal getUserPrincipal() {
      return principal;
    }

    @Override
    public boolean isSecure() {
      return delegate.isSecure();
    }

    @Override
    public boolean isUserInRole(String role) {
      return principal.getGroups().contains(role);
    }
  }

  static class BearerTokenExtractor extends AbstractBearerTokenExtractor {
    private final ContainerRequestContext requestContext;

    BearerTokenExtractor(ContainerRequestContext requestContext,
        JWTAuthContextInfo authContextInfo) {
      super(authContextInfo);
      this.requestContext = requestContext;
    }

    @Override
    protected String getCookieValue(String cookieName) {
      Cookie tokenCookie = requestContext.getCookies().get(cookieName);
      if (tokenCookie != null) {
        return tokenCookie.getValue();
      }
      return null;
    }

    @Override
    protected String getHeaderValue(String headerName) {
      return requestContext.getHeaderString(headerName);
    }
  }
}

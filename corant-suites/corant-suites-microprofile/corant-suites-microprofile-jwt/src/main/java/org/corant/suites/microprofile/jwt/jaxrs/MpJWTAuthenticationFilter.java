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
package org.corant.suites.microprofile.jwt.jaxrs;

import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;

/**
 * corant-microprofile-jwt
 *
 * @author bingo 下午2:37:35
 *
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class MpJWTAuthenticationFilter extends JWTAuthenticationFilter {

  public static final String JTW_EXCEPTION_KEY = "___JWT-EX___";

  private static Logger logger = Logger.getLogger(MpJWTAuthenticationFilter.class);

  @Inject
  private JWTParser jwtParser;

  @Inject
  private JWTAuthContextInfo authContextInfo;

  @Inject
  private PrincipalProducer producer;

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
          JsonWebToken jwtPrincipal = jwtParser.parse(bearerToken);
          producer.setJsonWebToken(jwtPrincipal);
          // Install the JWT principal as the caller
          requestContext.setSecurityContext(new JWTSecurityContext(securityContext, jwtPrincipal));
          logger.debugf("Success");
        } catch (Exception e) {
          logger.warnf("Unable to parse/validate JWT: %s.", e.getMessage());
          requestContext.setProperty(JTW_EXCEPTION_KEY, e);
        }
      }
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

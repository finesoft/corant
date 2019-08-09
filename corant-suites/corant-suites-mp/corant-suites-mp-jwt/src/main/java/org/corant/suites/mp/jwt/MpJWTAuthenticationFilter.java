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
package org.corant.suites.mp.jwt;

import static org.corant.kernel.util.Instances.resolve;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 上午10:32:22
 *
 */
@Priority(Priorities.AUTHENTICATION)
public class MpJWTAuthenticationFilter extends JWTAuthenticationFilter {

  private static Logger logger = Logger.getLogger(MpJWTAuthenticationFilter.class);

  static JWTAuthContextInfo authContextInfo() {
    return resolve(JWTAuthContextInfo.class).get();
  }

  static PrincipalProducer principalProducer() {
    return resolve(PrincipalProducer.class).get();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (!MpPermitAllFilter.PERMITALL_VAL
        .equals(requestContext.getProperty(MpPermitAllFilter.PERMITALL_KEY))) {
      final SecurityContext securityContext = requestContext.getSecurityContext();
      final Principal principal = securityContext.getUserPrincipal();
      try {
        if (!(principal instanceof JsonWebToken)) {
          AbstractBearerTokenExtractor extractor =
              new BearerTokenExtractor(requestContext, authContextInfo());
          String bearerToken = extractor.getBearerToken();
          if (bearerToken != null) {
            JsonWebToken jwtPrincipal = extractor.validate(bearerToken);
            principalProducer().setJsonWebToken(jwtPrincipal);
            // Install the JWT principal as the caller
            MpJWTSecurityContext jwtSecurityContext =
                new MpJWTSecurityContext(securityContext, jwtPrincipal);
            requestContext.setSecurityContext(jwtSecurityContext);
            logger.debugf("Success");
          }
        }
      } catch (Exception e) {
        logger.warnf(e, "Unable to parse/validate JWT: %s", e.getMessage());
        requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
      } finally {
        requestContext.removeProperty(MpPermitAllFilter.PERMITALL_KEY);
      }
    } else {
      requestContext.removeProperty(MpPermitAllFilter.PERMITALL_KEY);
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

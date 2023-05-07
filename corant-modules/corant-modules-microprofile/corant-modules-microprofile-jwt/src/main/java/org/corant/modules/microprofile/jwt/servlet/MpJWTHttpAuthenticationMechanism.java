/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.microprofile.jwt.servlet;

import java.io.IOException;
import java.util.Set;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * corant-modules-microprofile-jwt
 *
 * <p>
 * Note: Code base from smallrye jwt, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * @author bingo 下午2:38:48
 *
 */
public class MpJWTHttpAuthenticationMechanism extends AbstractMPJWTHttpServletAuthentication
    implements HttpAuthenticationMechanism {

  protected static final Logger logger = Logger.getLogger(MpJWTHttpAuthenticationMechanism.class);
  protected static final boolean debugLogging = logger.isDebugEnabled();

  @Override
  public AuthenticationStatus validateRequest(HttpServletRequest request,
      HttpServletResponse response, HttpMessageContext httpMessageContext)
      throws AuthenticationException {
    String bearerToken = resolveToken(request);
    if (bearerToken != null) {
      try {
        JsonWebToken jwtPrincipal = authenticate(bearerToken, request);
        Set<String> groups = jwtPrincipal.getGroups();
        return httpMessageContext.notifyContainerAboutLogin(jwtPrincipal, groups);
      } catch (org.corant.modules.security.AuthenticationException e) {
        if (debugLogging) {
          logger.warnf(e, "Unable to parse/validate JWT: %s.", e.getMessage());
        } else {
          logger.warnf("Unable to parse/validate JWT: %s.", e.getMessage());
        }
        return httpMessageContext.responseUnauthorized();
      } catch (Exception e) {
        return reportInternalError(httpMessageContext);
      }
    } else {
      return httpMessageContext.isProtected() ? httpMessageContext.responseUnauthorized()
          : httpMessageContext.doNothing();
    }
  }

  protected AuthenticationStatus reportInternalError(HttpMessageContext httpMessageContext) {
    try {
      httpMessageContext.getResponse().sendError(500);
    } catch (IOException ioException) {
      throw new IllegalStateException(ioException);
    }
    return AuthenticationStatus.SEND_FAILURE;
  }

}

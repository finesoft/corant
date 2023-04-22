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
package org.corant.modules.microprofile.jwt.jaxrs;

import java.io.IOException;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
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
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;

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
public class MpJWTHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

  protected static final Logger logger = Logger.getLogger(MpJWTHttpAuthenticationMechanism.class);
  protected static final boolean debugLogging = logger.isDebugEnabled();

  @Inject
  protected JWTAuthContextInfo authContextInfo;

  @Inject
  protected JWTParser jwtParser;

  @Inject
  protected PrincipalProducer producer;

  @Inject
  @Any
  protected Instance<Authenticator> authenticatorInstance;

  @Inject
  @Any
  protected Instance<SecurityContextManager<HttpServletRequest>> securityManagers;

  protected Authenticator authenticator = MpJWTAuthenticator.DFLT_INST;

  @Override
  public AuthenticationStatus validateRequest(HttpServletRequest request,
      HttpServletResponse response, HttpMessageContext httpMessageContext)
      throws AuthenticationException {
    AbstractBearerTokenExtractor extractor = new BearerTokenExtractor(request, authContextInfo);
    String bearerToken = extractor.getBearerToken();
    if (bearerToken != null) {
      try {
        AuthenticationData authcData =
            authenticator.authenticate(new MpJWTJsonWebToken(bearerToken, request));
        JsonWebToken jwtPrincipal = authcData.getPrincipal(MpJWTPrincipal.class);
        producer.setJsonWebToken(jwtPrincipal);
        Set<String> groups = jwtPrincipal.getGroups();
        request.setAttribute(JsonWebToken.class.getCanonicalName(), jwtPrincipal);
        if (securityManagers.isResolvable()) {
          securityManagers.get().bind(request);
        }
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

  @PostConstruct
  protected void onPostConstruct() {
    if (authenticatorInstance.isResolvable()) {
      authenticator = authenticatorInstance.get();
    } else if (!authenticatorInstance.isUnsatisfied()) {
      authenticator = authenticatorInstance.stream().sorted(Sortable::compare).findFirst().get();
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

  protected static class BearerTokenExtractor extends AbstractBearerTokenExtractor {

    private final HttpServletRequest request;

    BearerTokenExtractor(HttpServletRequest request, JWTAuthContextInfo authContextInfo) {
      super(authContextInfo);
      this.request = request;
    }

    @Override
    protected String getCookieValue(String cookieName) {
      Cookie[] cookies = request.getCookies();
      Cookie tokenCookie = null;
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (cookieName.equals(cookie.getName())) {
            tokenCookie = cookie;
            break;
          }
        }
      }
      if (tokenCookie != null) {
        return tokenCookie.getValue();
      }
      return null;
    }

    @Override
    protected String getHeaderValue(String headerName) {
      return request.getHeader(headerName);
    }
  }

}

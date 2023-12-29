/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.corant.modules.microprofile.jwt.MpJWTAuthenticator;
import org.corant.modules.microprofile.jwt.MpJWTJsonWebToken;
import org.corant.modules.microprofile.jwt.MpJWTPrincipal;
import org.corant.modules.security.AuthenticationData;
import org.corant.modules.security.Authenticator;
import org.corant.modules.security.SecurityContextManager;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午11:23:46
 */
public abstract class AbstractMPJWTHttpServletAuthentication {

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

  protected JsonWebToken authenticate(String token, HttpServletRequest request) {
    AuthenticationData authcData =
        authenticator.authenticate(new MpJWTJsonWebToken(token, request));
    JsonWebToken jwtPrincipal = authcData.getPrincipal(MpJWTPrincipal.class);
    producer.setJsonWebToken(jwtPrincipal);
    request.setAttribute(JsonWebToken.class.getCanonicalName(), jwtPrincipal);
    if (securityManagers.isResolvable()) {
      securityManagers.get().bind(request);
    }
    return jwtPrincipal;
  }

  @PostConstruct
  protected void onPostConstruct() {
    if (authenticatorInstance.isResolvable()) {
      authenticator = authenticatorInstance.get();
    } else if (!authenticatorInstance.isUnsatisfied()) {
      authenticator = authenticatorInstance.stream().sorted(Sortable::compare).findFirst().get();
    }
  }

  protected String resolveToken(HttpServletRequest request) {
    return new BearerTokenExtractor(request, authContextInfo).getBearerToken();
  }

  /**
   * corant-modules-microprofile-jwt
   *
   * @author bingo 上午11:38:16
   *
   */
  protected static class BearerTokenExtractor extends AbstractBearerTokenExtractor {

    private final HttpServletRequest request;

    protected BearerTokenExtractor(HttpServletRequest request, JWTAuthContextInfo authContextInfo) {
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

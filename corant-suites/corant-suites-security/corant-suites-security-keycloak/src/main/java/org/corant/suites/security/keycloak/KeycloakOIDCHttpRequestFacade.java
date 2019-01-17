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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.common.util.HostUtils;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午4:51:37
 *
 */
public class KeycloakOIDCHttpRequestFacade implements OIDCHttpFacade.Request {

  final ContainerRequestContext requestContext;
  final boolean secure;

  /**
   * @param requestContext
   * @param secure
   */
  public KeycloakOIDCHttpRequestFacade(ContainerRequestContext requestContext, boolean secure) {
    super();
    this.requestContext = requestContext;
    this.secure = secure;
  }

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
    MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
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
    return HostUtils.getIpAddress();
  }

  @Override
  public String getURI() {
    return requestContext.getUriInfo().getRequestUri().toString();
  }

  @Override
  public boolean isSecure() {
    return secure;
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

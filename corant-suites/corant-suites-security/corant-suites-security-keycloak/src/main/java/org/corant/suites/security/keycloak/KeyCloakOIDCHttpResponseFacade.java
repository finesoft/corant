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

import java.io.OutputStream;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.keycloak.adapters.OIDCHttpFacade;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午4:51:37
 *
 */
public class KeyCloakOIDCHttpResponseFacade implements OIDCHttpFacade.Response {

  final ContainerRequestContext requestContext;
  final ResponseBuilder responseBuilder = javax.ws.rs.core.Response.status(204);
  boolean responseFinished = false;

  /**
   * @param requestContext
   */
  public KeyCloakOIDCHttpResponseFacade(ContainerRequestContext requestContext) {
    super();
    this.requestContext = requestContext;
  }

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
    throw new IllegalStateException("Not supported yet");
  }

  public boolean isResponseFinished() {
    return responseFinished;
  }

  @Override
  public void resetCookie(String name, String path) {
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

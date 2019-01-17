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

import javax.security.cert.X509Certificate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午5:12:50
 *
 */
public class KeycloakOIDCHttpFacade implements OIDCHttpFacade {

  protected final ContainerRequestContext requestContext;
  protected final SecurityContext securityContext;
  protected final KeycloakOIDCHttpRequestFacade requestFacade;
  protected final KeycloakOIDCHttpResponseFacade responseFacade;
  protected KeycloakSecurityContext keycloakSecurityContext;
  protected boolean responseFinished;

  public KeycloakOIDCHttpFacade(ContainerRequestContext containerRequestContext,
      SecurityContext securityContext) {
    requestContext = containerRequestContext;
    this.securityContext = securityContext;
    requestFacade =
        new KeycloakOIDCHttpRequestFacade(containerRequestContext, securityContext.isSecure());
    responseFacade = new KeycloakOIDCHttpResponseFacade(containerRequestContext);
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

}

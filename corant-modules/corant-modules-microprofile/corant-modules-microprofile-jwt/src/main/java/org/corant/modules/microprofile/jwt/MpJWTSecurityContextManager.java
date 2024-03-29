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
package org.corant.modules.microprofile.jwt;

import static org.corant.shared.util.Assertions.shouldInstanceOf;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 下午4:49:16
 *
 */
@RequestScoped
public class MpJWTSecurityContextManager
    extends AbstractJWTSecurityContextManager<ContainerRequestContext> {

  @Override
  public void bind(ContainerRequestContext requestContext) {
    JsonWebToken userPrincipal = null;
    String authSchema = null;
    SecurityContext securityContext = requestContext.getSecurityContext();
    if (securityContext != null && securityContext.getUserPrincipal() != null) {
      userPrincipal = shouldInstanceOf(securityContext.getUserPrincipal(), JsonWebToken.class);
      authSchema = securityContext.getAuthenticationScheme();
    }
    bindJsonWebToken(userPrincipal, authSchema);
  }

  @PreDestroy
  protected void onPreDestroy() {
    unbind();
  }
}

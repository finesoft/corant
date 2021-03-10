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
package org.corant.suites.jaxrs.shared;

import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.corant.context.SecurityContext.DefaultSecurityContext;
import org.corant.context.SecurityContext.SecurityContexts;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-jaxrs-resteasy
 *
 * @author bingo 下午5:49:01
 *
 */
@ApplicationScoped
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION + 16)
public class SecurityContextFilter implements ContainerRequestFilter {

  @Inject
  @ConfigProperty(name = "jaxrs.security.context.enable", defaultValue = "true")
  protected boolean enable;

  @Inject
  @Any
  protected SecurityContextManager manager;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (enable) {
      manager.initialize(requestContext.getSecurityContext());
    }
  }

  @RequestScoped
  public static class SecurityContextManager {

    static final Logger logger = Logger.getLogger(SecurityContextManager.class.getName());

    void initialize(SecurityContext securityContext) {
      if (securityContext != null) {
        logger.fine(() -> "Initialize current JAXRS security context to SecurityContexts.");
        SecurityContexts.setCurrent(new DefaultSecurityContext(securityContext,
            securityContext.getAuthenticationScheme(), null, securityContext.getUserPrincipal()));
      } else {
        logger.fine(() -> "Initialize empty security context to SecurityContexts.");
        SecurityContexts.setCurrent(null);
      }
    }

    @PreDestroy
    void onPreDestroy() {
      logger.fine(() -> "Clean current security context from SecurityContexts.");
      SecurityContexts.setCurrent(null);
    }
  }
}

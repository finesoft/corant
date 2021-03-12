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

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import org.jboss.logging.Logger;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 上午11:33:50
 *
 */
@Priority(Priorities.AUTHENTICATION + 1)
public class MpJWTBlackListFilter implements ContainerRequestFilter {

  private static Logger logger = Logger.getLogger(MpJWTBlackListFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) {
    CDI.current().select(MpJWTBlackListFilterHandler.class).forEach(h -> h.handle(requestContext));
    logger.debugf("Blacklist filter handle successfully");
  }

}

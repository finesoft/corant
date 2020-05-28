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
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.corant.suites.servlet.abstraction.AbstractCorsFilter;

/**
 * corant-suites-jaxrs-shared
 *
 * @author bingo 下午2:51:41
 */
// @Provider
// @PreMatching
// @Priority(Priorities.AUTHORIZATION)
// @ApplicationScoped
public abstract class AbstractJaxrsCorsFilter extends AbstractCorsFilter
    implements ContainerRequestFilter, ContainerResponseFilter {

  // TODO FIXME
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

  }

  // TODO FIXME
  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) throws IOException {
    if (isEnabled()) {
      responseContext.getHeaders().add("Access-Control-Allow-Origin", getOrigin());
      responseContext.getHeaders().add("Access-Control-Allow-Headers", getHeaders());
      responseContext.getHeaders().add("Access-Control-Allow-Credentials", getCredentials());
      responseContext.getHeaders().add("Access-Control-Allow-Methods", getMethods());
      responseContext.getHeaders().add("Access-Control-Max-Age", getMaxAge());
    }
  }
}
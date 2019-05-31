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
package org.corant.suites.jaxrs.resteasy;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import org.corant.kernel.normal.Defaults;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.core.interception.PreMatchContainerRequestContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.spi.HttpRequest;

/**
 * corant-suites-jaxrs-resteasy
 *
 * @author bingo 下午5:49:01
 *
 */
@ApplicationScoped
@Provider
@PreMatching
public class ResteasyCharsetContainerRequestFilter implements ContainerRequestFilter {

  @Inject
  @ConfigProperty(name = "servlet.request-charset-filter.enable", defaultValue = "true")
  boolean enable;

  @Inject
  @ConfigProperty(name = "servlet.request-charset-filter.charset",
      defaultValue = Defaults.DFLT_CHARSET_STR)
  String charset;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (requestContext instanceof PreMatchContainerRequestContext && enable) {
      PreMatchContainerRequestContext reqctx =
          PreMatchContainerRequestContext.class.cast(requestContext);
      HttpRequest hr = reqctx.getHttpRequest();
      hr.setAttribute(InputPart.DEFAULT_CHARSET_PROPERTY, charset);
    }
  }

}

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
package org.corant.modules.jaxrs.resteasy;

import java.io.IOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.corant.shared.normal.Defaults;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.spi.HttpRequest;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 下午5:49:01
 */
@ApplicationScoped
@Provider
@PreMatching
public class ResteasyMultiPartCharsetFilter implements ContainerRequestFilter {

  @Inject
  @ConfigProperty(name = "corant.resteasy.multi-part.request-charset-filter.enable",
      defaultValue = "true")
  protected boolean enable;

  @Inject
  @ConfigProperty(name = "corant.resteasy.multi-part.request-charset-filter.charset",
      defaultValue = Defaults.DFLT_CHARSET_STR)
  protected String charset;

  @Inject
  @ConfigProperty(name = "corant.resteasy.multi-part.request-charset-filter.context-type",
      defaultValue = "text/plain; charset=" + Defaults.DFLT_CHARSET_STR)
  protected String contextType;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (requestContext instanceof PreMatchContainerRequestContext && enable) {
      PreMatchContainerRequestContext reqctx = (PreMatchContainerRequestContext) requestContext;
      HttpRequest hr = reqctx.getHttpRequest();
      hr.setAttribute(InputPart.DEFAULT_CONTENT_TYPE_PROPERTY, contextType);
      hr.setAttribute(InputPart.DEFAULT_CHARSET_PROPERTY, charset);
    }
  }

}

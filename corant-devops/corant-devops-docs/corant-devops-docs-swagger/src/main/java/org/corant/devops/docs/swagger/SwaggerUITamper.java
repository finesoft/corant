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
package org.corant.devops.docs.swagger;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;

/**
 * corant-devops-docs-swagger
 *
 * @author bingo 下午3:18:12
 *
 */
@ApplicationScoped
@RequiredConfiguration(key = "corant.devops.docs.swagger.openapi.enable",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
@WebFilter(filterName = "SwaggerUITamper",
    urlPatterns = {"${corant.devops.docs.swagger.openapi.visit-path}*"})
public class SwaggerUITamper implements Filter {

  static final String jsfmt = "window.onload = function() { " + " window.ui = SwaggerUIBundle({"
      + "url: \"%s/openapi.json\"," + "dom_id: '#swagger-ui'," + "deepLinking: true,"
      + "presets: [SwaggerUIBundle.presets.apis,SwaggerUIStandalonePreset],"
      + "plugins: [SwaggerUIBundle.plugins.DownloadUrl]," + "layout: \"StandaloneLayout\" " + "});"
      + "};";

  @Inject
  protected ResteasyProvider restProvider;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String pathInfo = ((HttpServletRequest) request).getRequestURI();
    if (pathInfo.endsWith("swagger-initializer.js")) {
      response.setContentType("application/javascript");
      response.getWriter()
          .append(String.format(jsfmt, restProvider.getApplicationInfo().getApplicationPath()));
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

}

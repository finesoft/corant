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

import static java.lang.String.format;
import static org.corant.shared.util.Strings.isBlank;
import java.io.IOException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider;
import org.corant.modules.jaxrs.resteasy.ResteasyResource;
import org.corant.shared.resource.URLResource;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;
import org.corant.shared.util.Resources;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-devops-docs-swagger
 *
 * @author bingo 下午3:03:08
 */
@ApplicationScoped
@Path("/openapi-ui")
@RequiredConfiguration(key = "corant.devops.docs.swagger.openapi.enable",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
public class SwaggerOpenApiUIResource extends ResteasyResource {

  static final String initjs = "window.onload = function() { " + " window.ui = SwaggerUIBundle({"
      + "url: \"%s/openapi.json\"," + "dom_id: '#swagger-ui'," + "deepLinking: true,"
      + "presets: [SwaggerUIBundle.presets.apis,SwaggerUIStandalonePreset],"
      + "plugins: [SwaggerUIBundle.plugins.DownloadUrl]," + "layout: \"StandaloneLayout\" " + "});"
      + "};";

  @Inject
  @ConfigProperty(name = "corant.devops.docs.swagger.openapi.ui.content.path")
  protected String contentRoot;

  @Inject
  protected ResteasyProvider restProvider;

  @GET
  @Path("{path:.*}")
  public Response get(@PathParam("path") String path) throws IOException {
    String realPath = contentRoot + (isBlank(path) || "/".equals(path) ? "index.html" : path);
    if (realPath.endsWith("/swagger-initializer.js")) {
      return Response.ok(format(initjs, restProvider.getApplicationInfo().getApplicationPath()),
          "application/javascript").build();
    } else {
      URLResource resource = Resources.from(realPath).findFirst().orElse(null);
      if (resource != null) {
        return Response.ok(resource.openInputStream()).build();
      } else {
        return Response.status(Status.NOT_FOUND).build();
      }
    }
  }
}

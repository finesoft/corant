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

import static org.corant.config.Configs.resolveSingle;
import static org.corant.context.Beans.resolve;
import java.util.logging.Logger;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.required.RequiredExtension;
import org.corant.kernel.event.PostCorantReadyAsyncEvent;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider.ApplicationInfo;
import org.corant.shared.util.Systems;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;

/**
 * corant-devops-docs-swagger
 *
 * @author bingo 下午3:18:03
 */
public class SwaggerOpenApiExtension implements Extension {

  static final Logger logger = Logger.getLogger(SwaggerOpenApiExtension.class.getName());
  static final String visitPath = "/openapi-ui/index.html";
  static final CorantSwaggerConfiguration config = resolveSingle(CorantSwaggerConfiguration.class);

  protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event) {
    if (config == null) {
      RequiredExtension.addVeto(BaseOpenApiResource.class);
    } else {
      Systems.setProperty("corant.resteasy.application.alternative-if-unresolved",
          SwaggerOpenApiApp.class.getCanonicalName());
    }
  }

  protected void onPostCorantReadyEvent(@ObservesAsync PostCorantReadyAsyncEvent adv) {
    if (config != null) {
      try {
        ApplicationInfo appInfo = resolve(ResteasyProvider.class).getApplicationInfo();
        logger.info(() -> String.format("Initialize swagger open api context, the visit path is %s",
            appInfo != null ? appInfo.getApplicationPath() + visitPath : visitPath));
        JaxrsOpenApiContextBuilder<?> builder = new JaxrsOpenApiContextBuilder<>();
        builder.openApiConfiguration(config);
        if (appInfo != null) {
          builder.setApplication(appInfo.getApplication());
        }
        builder.buildContext(true);
      } catch (OpenApiConfigurationException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  @ConfigKeyRoot(value = "corant.devops.docs.swagger.openapi", ignoreNoAnnotatedItem = false)
  public static class CorantSwaggerConfiguration extends SwaggerConfiguration
      implements DeclarativeConfig {

    private static final long serialVersionUID = -8860138727231236968L;

    private boolean enable;

    public boolean isEnable() {
      return enable;
    }

    @Override
    public boolean isValid() {
      return enable;
    }

  }
}

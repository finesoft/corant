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

import static org.corant.context.Beans.resolve;
import java.util.Set;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider;
import org.corant.modules.jaxrs.resteasy.ResteasyProvider.ApplicationInfo;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;

/**
 * corant-devops-docs-swagger
 *
 * @author bingo 下午4:48:20
 *
 */
public class SwaggerOpenApiJaxrsResourceScanner extends JaxrsApplicationAndAnnotationScanner {

  @Override
  public Set<Class<?>> classes() {
    Set<Class<?>> classes = super.classes();
    ApplicationInfo appInfo = resolve(ResteasyProvider.class).getApplicationInfo();
    if (SwaggerOpenApiExtension.config != null && appInfo != null
        && !appInfo.getApplicationClass().equals(SwaggerOpenApiApp.class)) {
      classes.remove(SwaggerOpenApiApp.class);
    }
    return classes;
  }

}

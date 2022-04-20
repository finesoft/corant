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

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * corant-devops-docs-swagger
 *
 * @author bingo 下午3:18:07
 *
 */
@ApplicationPath("/devops/docs")
@LoginConfig(authMethod = "MP-JWT")
@OpenAPIDefinition(
    info = @Info(title = "Corant web api docs", version = "1.0", description = "Corant DevOps"))
@SecurityScheme(name = "MP-JWT", scheme = "bearer", type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class SwaggerOpenApiApp extends Application {

}

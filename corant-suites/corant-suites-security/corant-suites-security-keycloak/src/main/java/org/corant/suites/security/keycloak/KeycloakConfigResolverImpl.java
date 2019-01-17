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
package org.corant.suites.security.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassPaths;
import org.corant.shared.util.ClassPaths.ResourceInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade.Request;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午4:49:32
 *
 */
@ApplicationScoped
public class KeycloakConfigResolverImpl implements KeycloakConfigResolver {

  KeycloakDeployment deployment;

  @Inject
  @ConfigProperty(name = "secutiry.keycloak.deployment-file-path",
      defaultValue = "META-INF/keycloak.json")
  String deploymentFilePath;

  @Override
  public KeycloakDeployment resolve(Request facade) {
    return deployment;
  }

  boolean isConfigured() {
    return deployment != null && deployment.isConfigured();
  }

  @PostConstruct
  void onPostConstruct() {
    URL kcf;
    kcf = ClassPaths.anyway(deploymentFilePath).getResources().map(ResourceInfo::getUrl).findFirst()
        .orElse(null);
    if (kcf != null) {
      try (InputStream is = kcf.openStream()) {
        deployment = KeycloakDeploymentBuilder.build(is);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}

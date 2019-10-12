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
package org.corant.suites.keycloak.adminclient;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.MapUtils.getMapString;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.Resource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;

/**
 * corant-suites-keycloak-admin
 *
 * @author bingo 上午11:22:03
 *
 */
@ApplicationScoped
public class KeycloakAdmin {

  @Inject
  Logger logger;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.json-path")
  protected Optional<String> keycloakJsonPath;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.username")
  protected Optional<String> username;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.password")
  protected Optional<String> password;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.server-url")
  protected Optional<String> serverUrl;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.realm")
  protected Optional<String> realm;

  @Inject
  @ConfigProperty(name = "keycloak.admin-client.client-id")
  protected Optional<String> clientId;

  @Inject
  @Any
  protected Instance<KeycloakAdminResteasyClientProvider> resteasyClientProvider;

  protected AdapterConfig adminConfig;

  @PostConstruct
  void onPostConstruct() {
    synchronized (this) {
      if (keycloakJsonPath.isPresent()) {
        Resource resource =
            Resources.tryFromClassPath(keycloakJsonPath.get()).findFirst().orElse(null);
        if (resource != null) {
          logger.info(() -> String.format("Find keycloak admin client config json %s",
              keycloakJsonPath.get()));
          try (InputStream is = resource.openStream()) {
            adminConfig = JsonSerialization.readValue(is, AdapterConfig.class);
          } catch (IOException e) {
            throw new CorantRuntimeException(e);
          }
        }
      }
    }
  }

  @Produces
  Keycloak produce() {
    if (adminConfig != null) {
      logger.info(() -> "Use keycloak admin config json to build Keycloak instance.");
      KeycloakBuilder builder = KeycloakBuilder.builder().serverUrl(adminConfig.getAuthServerUrl())
          .realm(adminConfig.getRealm()).grantType(OAuth2Constants.CLIENT_CREDENTIALS)
          .clientId(adminConfig.getResource())
          .clientSecret(getMapString(adminConfig.getCredentials(), "secret"));
      if (resteasyClientProvider.isResolvable()) {
        builder.resteasyClient(resteasyClientProvider.get().get());
      }
      return builder.build();
    } else {
      shouldBeTrue(serverUrl.isPresent() && realm.isPresent() && username.isPresent()
          && password.isPresent() && clientId.isPresent());
      logger.info(() -> "Use keycloak admin config to build Keycloak instance.");
      KeycloakBuilder builder =
          KeycloakBuilder.builder().serverUrl(serverUrl.get()).realm(realm.get())
              .username(username.get()).password(password.get()).clientId(clientId.get());
      if (resteasyClientProvider.isResolvable()) {
        builder.resteasyClient(resteasyClientProvider.get().get());
      }
      return builder.build();
    }
  }

  public interface KeycloakAdminResteasyClientProvider {

    ResteasyClient get();
  }
}

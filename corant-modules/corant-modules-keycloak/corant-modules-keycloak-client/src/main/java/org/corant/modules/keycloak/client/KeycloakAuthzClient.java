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
package org.corant.modules.keycloak.client;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.client.methods.RequestBuilder;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.ServerConfiguration;
import org.keycloak.authorization.client.util.HttpMethod;
import org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

/**
 * corant-modules-keycloak-client
 *
 * @author bingo 下午3:10:47
 */
@ApplicationScoped
public class KeycloakAuthzClient {

  @Inject
  Logger logger;

  @Inject
  @ConfigProperty(name = "corant.keycloak.authz-client.json")
  protected Optional<String> keycloakJson;

  @Inject
  @ConfigProperty(name = "corant.keycloak.authz-client.location",
      defaultValue = "META-INF/keycloak-authz-client.json")
  protected String keycloakJsonLocation;

  protected AuthzClient authzClient = null;
  protected ServerConfiguration serverConfiguration = null;
  protected Configuration configuration = null;
  protected ClientCredentialsProvider clientCredentialsProvider;

  public AuthzClient getAuthzClient() {
    return authzClient;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public String getKeycloakJsonLocation() {
    return keycloakJsonLocation;
  }

  public ServerConfiguration getServerConfiguration() {
    return serverConfiguration;
  }

  public AccessTokenResponse grantAccessToken(String userName, String password) {
    return authzClient.obtainAccessToken(userName, password);
  }

  public AccessTokenResponse refreshAccessToken(String refreshToken) {
    RequestBuilder builder = RequestBuilder.post().setUri(serverConfiguration.getTokenEndpoint())
        .addHeader("Authorization", BasicAuthHelper.createHeader(configuration.getResource(),
            (String) configuration.getCredentials().get("secret")));
    HttpMethod<AccessTokenResponse> method =
        new HttpMethod<>(configuration, clientCredentialsProvider, builder);
    method.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
    method.param(OAuth2Constants.REFRESH_TOKEN, refreshToken);
    method.authorizationBearer(configuration.getCredentials().get("secret").toString());
    return method.response().json(AccessTokenResponse.class).execute();
  }

  @PostConstruct
  protected void onPostConstruct() {
    try {
      if (keycloakJson.isPresent() && isNotBlank(keycloakJson.get())) {
        authzClient = AuthzClient
            .create(JsonSerialization.readValue(keycloakJson.get(), Configuration.class));
        logger.fine(() -> format("Create keycloak authz client instance %s.", keycloakJson));
      } else {
        authzClient = AuthzClient
            .create(Resources.from(keycloakJsonLocation).findFirst().get().openInputStream());
        logger
            .fine(() -> format("Create keycloak authz client instance %s.", keycloakJsonLocation));
      }
      configuration = authzClient.getConfiguration();
      serverConfiguration = authzClient.getServerConfiguration();
      clientCredentialsProvider = new SimpleClientCredentialsProvider(configuration);
      // http = new Http(configuration, createDefaultClientAuthenticator(configuration));
    } catch (RuntimeException | IOException e) {
      throw new CorantRuntimeException(e, "Can't find keycloak.json from %s.",
          keycloakJsonLocation);
    }
  }

  class SimpleClientCredentialsProvider implements ClientCredentialsProvider {

    public static final String PROVIDER_ID = CredentialRepresentation.SECRET;

    private final String clientSecret;

    SimpleClientCredentialsProvider(Configuration config) {
      clientSecret = shouldNotNull((String) config.getCredentials().get("secret"));
    }

    @Override
    public String getId() {
      return PROVIDER_ID;
    }

    @Override
    public void init(AdapterConfig deployment, Object config) {}

    @Override
    public void setClientCredentials(AdapterConfig deployment, Map<String, String> requestHeaders,
        Map<String, String> formParams) {
      String clientId = deployment.getResource();

      if (!deployment.isPublicClient()) {
        if (clientSecret != null) {
          String authorization = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
          requestHeaders.put("Authorization", authorization);
        } else {
          logger.warning(format("Client '%s' doesn't have secret available", clientId));
        }
      } else {
        formParams.put(OAuth2Constants.CLIENT_ID, clientId);
      }
    }
  }
}

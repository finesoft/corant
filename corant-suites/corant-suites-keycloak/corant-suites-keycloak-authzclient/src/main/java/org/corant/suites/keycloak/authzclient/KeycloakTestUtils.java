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
package org.corant.suites.keycloak.authzclient;

import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.mapOf;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.ClassPathResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 下午5:12:10
 *
 */
public class KeycloakTestUtils {

  public static final String OID_CONN_URL = "/protocol/openid-connect/token";

  public static String getAccessToken(String username, String password) {
    return getMapString(oidcToken(username, password), "access_token");
  }

  public static Map<?, ?> oidcToken(String username, String password) {
    Pair<URL, Map<String, String>> params = oidcTokenParam(username, password);
    if (!params.isEmpty()) {
      Form form = new Form();
      params.getValue().forEach(form::param);
      try {
        Client client = ClientBuilder.newClient();
        Map<?, ?> map =
            client.target(params.getLeft().toURI()).request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Map.class);
        client.close();
        return map;
      } catch (URISyntaxException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return null;
  }

  public static Pair<URL, Map<String, String>> oidcTokenParam(String username, String password) {
    AdapterConfig ac = resolveConfigs();
    if (ac != null) {
      String url = ac.getAuthServerUrl() + "/realms/" + ac.getRealm() + OID_CONN_URL;
      String client_secret = getMapString(ac.getCredentials(), "secret");
      String client_id = ac.getResource();
      try {
        return Pair.of(new URL(url), mapOf("client_id", client_id, "client_secret", client_secret,
            "username", username, "password", password, "grant_type", "password"));
      } catch (MalformedURLException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      return Pair.empty();
    }
  }

  public static AdapterConfig resolveConfigs() {
    String path = ConfigProvider.getConfig()
        .getOptionalValue("security.keycloak.config-file-path", String.class)
        .orElseGet(() -> "META-INF/keycloak.json");
    ClassPathResource cpr = Resources.tryFromClassPath(path).findFirst().orElseGet(null);
    if (cpr != null) {
      try (InputStream is = cpr.openStream()) {
        return KeycloakDeploymentBuilder.loadAdapterConfig(is);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return null;
  }
}

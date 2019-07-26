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
package corant.suites.keycloak.spi;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 上午10:19:05
 *
 */
public class KeycloakEventListenerProviderFactory implements EventListenerProviderFactory {

  final String id = "corant-keycloak-event-listener";

  @Override
  public void close() {

  }

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    KeycloakEventListenerProvider provider = new KeycloakEventListenerProvider((t) -> true);
    session.enlistForClose(provider);
    return provider;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void init(Scope config) {

  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {

  }

}

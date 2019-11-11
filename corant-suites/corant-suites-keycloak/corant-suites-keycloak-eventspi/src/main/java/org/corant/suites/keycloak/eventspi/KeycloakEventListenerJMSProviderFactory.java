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
package org.corant.suites.keycloak.eventspi;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 上午10:19:05
 *
 */
public class KeycloakEventListenerJMSProviderFactory implements EventListenerProviderFactory {
  static final Logger logger = Logger.getLogger(KeycloakEventListenerJMSProviderFactory.class);
  static final String id = "corantKeycloakEventListener";
  KeycloakEventSelector<Event> eventSelector;
  KeycloakEventSelector<AdminEvent> adminEventSelector;
  KeycloakJMSSender jmsSender;

  @Override
  public void close() {

  }

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new KeycloakEventListenerJMSProvider(eventSelector, adminEventSelector, jmsSender);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void init(Scope config) {
    Scope s = Config.scope("eventListener", id);
    String adminEventPredicates = s.get("admin-event-selectors");
    String eventPredicates = s.get("event-selectors");
    eventSelector = new KeycloakEventTypeSelector(eventPredicates);
    adminEventSelector = new KeycloakAdminEventSelector(adminEventPredicates);
    jmsSender = new KeycloakJMSSender(s);
    logger.infof("Initialize %s with id %s.", this.getClass().getName(), getId());
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {}

}

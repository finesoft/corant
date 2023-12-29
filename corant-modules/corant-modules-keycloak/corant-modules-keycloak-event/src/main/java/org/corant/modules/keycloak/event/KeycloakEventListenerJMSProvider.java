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
package org.corant.modules.keycloak.event;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

/**
 * corant-modules-keycloak-event
 *
 * @author bingo 上午10:16:59
 */
public class KeycloakEventListenerJMSProvider implements EventListenerProvider {

  static final Logger logger = Logger.getLogger(KeycloakEventListenerJMSProvider.class);
  KeycloakEventSelector<Event> eventTypeSelector;
  KeycloakEventSelector<AdminEvent> adminEventSelector;
  final KeycloakJMSSender jmsSender;

  public KeycloakEventListenerJMSProvider(KeycloakEventSelector<Event> eventTypeSelector,
      KeycloakEventSelector<AdminEvent> adminEventSelector, KeycloakJMSSender jmsSender) {
    this.eventTypeSelector = eventTypeSelector;
    this.adminEventSelector = adminEventSelector;
    this.jmsSender = jmsSender;
  }

  @Override
  public void close() {

  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    logger.debugf("Resend admin event to jms!");
    if (!adminEventSelector.test(event) || jmsSender == null) {
      return;
    }
    try {
      jmsSender.send(event);
    } catch (Exception e) {
      logger.warn("Can't send admin event to jms broker!", e);
    }
  }

  @Override
  public void onEvent(Event event) {
    logger.debugf("Resend event to jms!");
    if (!eventTypeSelector.test(event) || jmsSender == null) {
      return;
    }
    try {
      jmsSender.send(event);
    } catch (Exception e) {
      logger.warn("Can't send event to jms broker!", e);
    }
  }

}

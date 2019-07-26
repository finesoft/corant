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

import java.util.function.Predicate;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

/**
 * corant-suites-security-keycloak
 *
 * @author bingo 上午10:16:59
 *
 */
public class KeycloakEventListenerProvider implements EventListenerProvider {

  static final Logger logger = Logger.getLogger(KeycloakEventListenerProvider.class);

  final KeycloakJMSSender jmsSender = new KeycloakJMSSender();
  final Predicate<Object> filter;

  public KeycloakEventListenerProvider(Predicate<Object> filter) {
    this.filter = filter == null ? (t) -> true : filter;
  }

  @Override
  public void close() {

  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    if (!filter.test(event)) {
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
    if (!filter.test(event)) {
      return;
    }
    try {
      jmsSender.send(event);
    } catch (Exception e) {
      logger.warn("Can't send event to jms broker!", e);
    }
  }

}

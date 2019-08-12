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

import java.util.Map;
import java.util.function.Predicate;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 下午8:12:00
 *
 */
public class CompositionEventSelector extends AbstactSelector implements Predicate<Event> {

  static final Logger logger = Logger.getLogger(CompositionEventSelector.class);

  public CompositionEventSelector(Scope scope) {
    super(scope);
  }

  @Override
  public boolean test(Event t) {
    boolean forward = false;
    if (!conditions.isEmpty()) {
      for (Map<?, ?> cmd : conditions) {
        boolean forwardx = true;
        forwardx &= matchString(t.getType().name(), cmd, "type");
        forwardx &= matchString(t.getClientId(), cmd, "clientId");
        forwardx &= matchString(t.getRealmId(), cmd, "realmId");
        forwardx &= matchString(t.getUserId(), cmd, "userId");
        forwardx &= matchString(t.getSessionId(), cmd, "sessionId");
        forwardx &= matchString(t.getIpAddress(), cmd, "ipAddress");
        forwardx &= matchLong(() -> t.getTime(), cmd, "time");
        if (forward |= forwardx) {
          break;
        }
      }
    } else {
      forward = true;
    }
    return forward;
  }

}

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
package org.corant.suites.keycloak.eventspi.selector;

import java.util.Map;
import java.util.function.Predicate;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.admin.AdminEvent;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 下午8:12:00
 *
 */
public class CompositionAdminEventSelector extends AbstactSelector implements Predicate<AdminEvent> {

  static final Logger logger = Logger.getLogger(CompositionAdminEventSelector.class);

  public CompositionAdminEventSelector(Scope scope) {
    super(scope);
  }

  @Override
  public boolean test(AdminEvent t) {
    boolean forward = false;
    if (!conditions.isEmpty()) {
      for (Map<?, ?> cmd : conditions) {
        boolean forwardx = true;
        forwardx &= matchString(t.getRepresentation(), cmd, "representation");
        forwardx &= matchString(t.getResourcePath(), cmd, "resourcePath");
        forwardx &= matchString(t.getOperationType().name(), cmd, "operationType");
        forwardx &= matchString(t.getResourceType().name(), cmd, "resourceType");
        if (t.getAuthDetails() != null) {
          forwardx &= matchString(t.getAuthDetails().getClientId(), cmd, "authDetails.clientId");
          forwardx &= matchString(t.getAuthDetails().getIpAddress(), cmd, "authDetails.ipAddress");
          forwardx &= matchString(t.getAuthDetails().getRealmId(), cmd, "authDetails.realmId");
          forwardx &= matchString(t.getAuthDetails().getUserId(), cmd, "authDetails.userId");
        }
        forwardx &= matchString(t.getRealmId(), cmd, "realmId");
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

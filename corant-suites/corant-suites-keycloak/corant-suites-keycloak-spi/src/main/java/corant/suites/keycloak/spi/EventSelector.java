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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import com.google.common.base.Objects;

/**
 * corant-suites-keycloak-spi
 *
 * @author bingo 下午10:06:29
 *
 */
public class EventSelector implements Predicate<Event> {

  static final Logger logger = Logger.getLogger(EventSelector.class);

  private Set<EventType> types = new HashSet<>();

  private String realmId;

  private String clientId;

  private String userId;

  private String sessionId;

  private String ipAddress;

  public EventSelector(Scope scope) {
    if (scope != null) {
      String ts = scope.get("event-types");
      if (ts != null) {
        Arrays.stream(ts.split(",")).map(EventType::valueOf).forEach(types::add);
      }
      realmId = scope.get("event-realmId");
      clientId = scope.get("event-clientId");
      userId = scope.get("event-userId");
      sessionId = scope.get("event-sessionId");
      ipAddress = scope.get("event-ipAddress");
    }
    logger.infof("The event selector is %s", this);
  }

  @Override
  public boolean test(Event t) {
    boolean forward = t != null;
    if (!types.isEmpty()) {
      forward &= types.contains(t.getType());
    }
    if (forward && realmId != null) {
      forward &= Objects.equal(realmId, t.getRealmId());
    }
    if (forward && clientId != null) {
      forward &= Objects.equal(clientId, t.getClientId());
    }
    if (forward && userId != null) {
      forward &= Objects.equal(userId, t.getUserId());
    }
    if (forward && ipAddress != null) {
      forward &= Objects.equal(ipAddress, t.getIpAddress());
    }
    if (forward && sessionId != null) {
      forward &= Objects.equal(sessionId, t.getSessionId());
    }
    return forward;
  }

  @Override
  public String toString() {
    return "EventSelector [types=["
        + String.join(",", types.stream().map(t -> t.name()).collect(Collectors.toList()))
        + "], realmId=" + realmId + ", clientId=" + clientId + ", userId=" + userId + ", sessionId="
        + sessionId + ", ipAddress=" + ipAddress + "]";
  }

}

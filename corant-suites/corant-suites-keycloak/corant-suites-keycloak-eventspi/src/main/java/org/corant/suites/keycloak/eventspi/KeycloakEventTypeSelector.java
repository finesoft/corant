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

import java.util.function.Predicate;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import com.google.common.base.Objects;

/**
 * corant-suites-keycloak-eventspi
 *
 * @author bingo 上午10:15:18
 *
 */
public class KeycloakEventTypeSelector implements KeycloakEventSelector<Event> {

  protected static final Logger logger = Logger.getLogger(KeycloakEventTypeSelector.class);

  private final Predicate<Event> predicate;

  public KeycloakEventTypeSelector(String eventTypes) {
    Predicate<Event> p = e -> false;
    if (eventTypes != null) {
      for (String fs : KeycloakEventSelector.split(eventTypes, FIRST_SP)) {
        EventTypePredicate etp = EventTypePredicate.of(fs);
        if (etp != null) {
          logger.infof("[OR] Build event type predicate [%s]", etp);
          p = p.or(etp);
        }
      }
    }
    predicate = p;
  }

  @Override
  public boolean test(Event t) {
    return predicate.test(t);
  }

  static class EventTypePredicate implements KeycloakEventSelector<Event> {

    private final String realmId;
    private final EventType type;

    protected EventTypePredicate(String realmId, String type) {
      this.realmId = realmId;
      if (type != null && !type.isEmpty()) {
        this.type = EventType.valueOf(type);
      } else {
        this.type = null;
      }
    }

    protected static EventTypePredicate of(String events) {
      if (events != null) {
        String[] ss = KeycloakEventSelector.split(events, SECOND_SP);
        if (ss != null && ss.length > 0) {
          if (ss.length == 1) {
            return new EventTypePredicate(ss[0], null);
          } else {
            return new EventTypePredicate(ss[0], ss[1]);
          }
        }
      }
      return null;
    }

    @Override
    public boolean test(Event t) {
      boolean matched = true;
      if (realmId != null) {
        matched &= realmId.equals(t.getRealmId());
      }
      if (type != null && matched) {
        matched &= Objects.equal(type, t.getType());
      }
      return matched;
    }

    @Override
    public String toString() {
      return "EventTypePredicate [realmId=" + realmId + ", type=" + type + "]";
    }

  }
}

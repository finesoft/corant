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
import java.util.regex.Pattern;
import org.jboss.logging.Logger;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

/**
 * corant-suites-keycloak-eventspi
 *
 * @author bingo 上午10:43:33
 *
 */
public class KeycloakAdminEventSelector implements KeycloakEventSelector<AdminEvent> {

  protected static final Logger logger = Logger.getLogger(KeycloakEventListenerJMSProvider.class);

  private final Predicate<AdminEvent> predicate;

  public KeycloakAdminEventSelector(String predicates) {
    Predicate<AdminEvent> p = e -> false;
    if (predicates != null) {
      String[] events = KeycloakEventSelector.split(predicates, FIRST_SP);
      for (String event : events) {
        AdminEventPredicate aep =
            AdminEventPredicate.of(KeycloakEventSelector.split(event, SECOND_SP));
        if (aep != null) {
          logger.infof("[OR] Build admin event predicate [%s]", aep);
          p = p.or(aep);
        }
      }
    }
    predicate = p;
  }

  @Override
  public boolean test(AdminEvent t) {
    return predicate.test(t);
  }

  public static class AdminEventPredicate implements KeycloakEventSelector<AdminEvent> {

    private final String realmId;
    private final ResourceType resourceType;
    private final OperationType operationType;
    private final Pattern resourcePathPattern;

    /**
     * @param realmId
     * @param resourceType
     * @param operationType
     * @param resourcePathPattern
     */
    protected AdminEventPredicate(String realmId, String resourceType, String operationType,
        String resourcePathPattern) {
      super();
      this.realmId = realmId;
      if (resourceType != null && !resourceType.isEmpty()) {
        this.resourceType = ResourceType.valueOf(resourceType);
      } else {
        this.resourceType = null;
      }
      if (operationType != null && !operationType.isEmpty()) {
        this.operationType = OperationType.valueOf(operationType);
      } else {
        this.operationType = null;
      }
      if (resourcePathPattern != null && !resourcePathPattern.isEmpty()) {
        this.resourcePathPattern = Pattern.compile(resourcePathPattern);
      } else {
        this.resourcePathPattern = null;
      }
    }

    public static AdminEventPredicate of(String[] pros) {
      if (pros != null && pros.length > 0) {
        if (pros.length == 1) {
          return new AdminEventPredicate(pros[0], null, null, null);
        } else if (pros.length == 2) {
          return new AdminEventPredicate(pros[0], pros[1], null, null);
        } else if (pros.length == 3) {
          return new AdminEventPredicate(pros[0], pros[1], pros[2], null);
        } else {
          return new AdminEventPredicate(pros[0], pros[1], pros[2], pros[3]);
        }
      } else {
        return null;
      }
    }

    @Override
    public boolean test(AdminEvent t) {
      boolean matched = true;
      if (realmId != null) {
        matched &= realmId.equals(t.getRealmId());
      }
      if (resourceType != null && matched) {
        matched &= resourceType.equals(t.getResourceType());
      }
      if (operationType != null && matched) {
        matched &= operationType.equals(t.getOperationType());
      }
      if (resourcePathPattern != null && matched) {
        matched &= resourcePathPattern.matcher(t.getResourcePath()).matches();
      }
      return matched;
    }

    @Override
    public String toString() {
      return "AdminEventPredicate [realmId=" + realmId + ", resourceType=" + resourceType
          + ", operationType=" + operationType + ", resourcePathPattern=" + resourcePathPattern
          + "]";
    }

  }
}

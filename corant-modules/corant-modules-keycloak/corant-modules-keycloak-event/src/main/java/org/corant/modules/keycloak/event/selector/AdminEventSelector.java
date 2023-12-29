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
package org.corant.modules.keycloak.event.selector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.admin.AdminEvent;
import com.google.common.base.Objects;

/**
 * corant-modules-keycloak-event
 *
 * @author bingo 下午10:18:08
 */
public class AdminEventSelector implements Predicate<AdminEvent> {
  static final Logger logger = Logger.getLogger(AdminEventSelector.class);
  private String realmId;
  private Set<String> types = new HashSet<>();
  private Set<String> resourcePaths = new HashSet<>();
  private String resourcePathRegex;
  private String authDetailsRealmId;
  private String authDetailsClientId;
  private String authDetailsUserId;
  private String authDetailsIpAddress;
  private Pattern resourcePathPattern;

  public AdminEventSelector(Scope scope) {
    if (scope != null) {
      String ts = scope.get("admin-event-types");
      if (ts != null) {
        Arrays.stream(ts.split(",")).forEach(types::add);
      }
      realmId = scope.get("admin-event-realmId");
      authDetailsClientId = scope.get("admin-event-authdetails-clientId");
      authDetailsUserId = scope.get("admin-event-authdetails-userId");
      authDetailsRealmId = scope.get("admin-event-authdetails-realmId");
      authDetailsIpAddress = scope.get("admin-event-authdetails-ipAddress");
      resourcePathRegex = scope.get("admin-event-resourcePathRegex");
      if (resourcePathRegex != null && resourcePathRegex.trim().length() > 0) {
        resourcePathPattern = Pattern.compile(resourcePathRegex, Pattern.CASE_INSENSITIVE);
      }
    }
    logger.infof("The admin event selector is %s.", this);
  }

  @Override
  public boolean test(AdminEvent t) {
    boolean forward = (t != null);
    if (forward && !types.isEmpty()) {
      forward &= types.contains(t.getResourceType().name() + ":" + t.getOperationType().name());
    }
    if (forward && realmId != null) {
      forward &= Objects.equal(realmId, t.getRealmId());
    }
    if (forward && authDetailsClientId != null) {
      forward &= t.getAuthDetails() != null
          && Objects.equal(authDetailsClientId, t.getAuthDetails().getClientId());
    }
    if (forward && authDetailsRealmId != null) {
      forward &= t.getAuthDetails() != null
          && Objects.equal(authDetailsRealmId, t.getAuthDetails().getRealmId());
    }
    if (forward && authDetailsUserId != null) {
      forward &= t.getAuthDetails() != null
          && Objects.equal(authDetailsUserId, t.getAuthDetails().getUserId());
    }
    if (forward && authDetailsIpAddress != null) {
      forward &= t.getAuthDetails() != null
          && Objects.equal(authDetailsIpAddress, t.getAuthDetails().getIpAddress());
    }
    if (forward && !resourcePaths.isEmpty()) {
      forward &= resourcePaths.contains(t.getResourcePath());
    }
    if (forward && resourcePathPattern != null) {
      forward &= resourcePathPattern.matcher(t.getResourcePath()).matches();
    }
    return forward;
  }

  @Override
  public String toString() {
    return "KeycloakAdminEventSelector [realmId=" + realmId + ", types=[" + String.join(",", types)
        + "], resourcePaths=[" + String.join(",", resourcePaths) + "], authDetailsRealmId="
        + authDetailsRealmId + ", authDetailsClientId=" + authDetailsClientId
        + ", authDetailsUserId=" + authDetailsUserId + ", resourcePathRegex=" + resourcePathRegex
        + ", authDetailsIpAddress=" + authDetailsIpAddress + "]";
  }

}

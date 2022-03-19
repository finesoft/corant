/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.security.shared;

import static org.corant.shared.util.Lists.newArrayList;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.modules.security.AuthorizationData;
import org.corant.modules.security.Permission;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:17:41
 *
 */
public class SimpleAuthzData implements AuthorizationData, AttributeSet {

  protected List<Role> roles = Collections.emptyList();

  protected List<Permission> permissions = Collections.emptyList();

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public SimpleAuthzData(Collection<String> roles, Collection<String> permissions) {
    this(roles, permissions, null);
  }

  public SimpleAuthzData(Collection<String> roles, Collection<String> permissions,
      Map<String, ? extends Serializable> attributes) {
    if (roles != null) {
      this.roles = roles.stream().map(SimpleRole::of).collect(Collectors.toUnmodifiableList());
    }
    if (permissions != null) {
      this.permissions =
          permissions.stream().map(SimplePermission::of).collect(Collectors.toUnmodifiableList());
    }
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  public SimpleAuthzData(List<? extends Role> roles, List<? extends Permission> permission) {
    this(roles, permission, null);
  }

  public SimpleAuthzData(List<? extends Role> roles, List<? extends Permission> permissions,
      Map<String, ? extends Serializable> attributes) {
    this.roles = Collections.unmodifiableList(newArrayList(roles));
    this.permissions = Collections.unmodifiableList(newArrayList(permissions));
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected SimpleAuthzData() {}

  public static SimpleAuthzData ofPermissions(List<? extends Permission> permissions) {
    return ofPermissions(permissions, null);
  }

  public static SimpleAuthzData ofPermissions(List<? extends Permission> permissions,
      Map<String, ? extends Serializable> attributes) {
    return new SimpleAuthzData(null, permissions, attributes);
  }

  public static SimpleAuthzData ofPermissions(Map<String, ? extends Serializable> attributes,
      String... permissions) {
    return ofPermissions(
        Arrays.stream(permissions).map(SimplePermission::of).collect(Collectors.toList()),
        attributes);
  }

  public static SimpleAuthzData ofPermissions(String... permissions) {
    return ofPermissions(null, permissions);
  }

  public static SimpleAuthzData ofRoles(List<? extends Role> roles) {
    return ofRoles(roles, null);
  }

  public static SimpleAuthzData ofRoles(List<? extends Role> roles,
      Map<String, ? extends Serializable> attributes) {
    return new SimpleAuthzData(roles, null, attributes);
  }

  public static SimpleAuthzData ofRoles(Map<String, ? extends Serializable> attributes,
      String... roles) {
    return new SimpleAuthzData(
        Arrays.stream(roles).map(SimpleRole::of).collect(Collectors.toList()), null, attributes);
  }

  public static SimpleAuthzData ofRoles(String... roles) {
    return ofRoles(null, roles);
  }

  @Override
  public Map<String, ? extends Serializable> getAttributes() {
    return attributes;
  }

  @Override
  public List<Permission> getPermissions() {
    return permissions;
  }

  @Override
  public Collection<? extends Role> getRoles() {
    return roles;
  }

}

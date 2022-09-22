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
import org.corant.shared.ubiquity.AttributeSet;
import org.corant.shared.util.Objects;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:17:41
 *
 */
public class SimpleAuthzData implements AuthorizationData, AttributeSet {

  private static final long serialVersionUID = -7901225699993579260L;

  protected Collection<Role> roles = Collections.emptySet();

  protected Collection<Permission> permissions = Collections.emptySet();

  protected Map<String, ? extends Serializable> attributes = Collections.emptyMap();

  public SimpleAuthzData(Collection<? extends Role> roles,
      Collection<? extends Permission> permission) {
    this(roles, permission, null);
  }

  public SimpleAuthzData(Collection<? extends Role> roles,
      Collection<? extends Permission> permissions,
      Map<String, ? extends Serializable> attributes) {
    this.roles = roles == null ? Collections.emptySet()
        : roles.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableSet());
    this.permissions = permissions == null ? Collections.emptySet()
        : permissions.stream().filter(Objects::isNotNull).collect(Collectors.toUnmodifiableSet());
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  public SimpleAuthzData(List<String> roles, List<String> permissions) {
    this(roles, permissions, null);
  }

  public SimpleAuthzData(List<String> roles, List<String> permissions,
      Map<String, ? extends Serializable> attributes) {
    if (roles != null) {
      this.roles = roles.stream().filter(Objects::isNotNull).map(SimpleRole::of)
          .collect(Collectors.toUnmodifiableSet());
    }
    if (permissions != null) {
      this.permissions = permissions.stream().filter(Objects::isNotNull).map(SimplePermission::of)
          .collect(Collectors.toUnmodifiableSet());
    }
    if (attributes != null) {
      this.attributes = Collections.unmodifiableMap(attributes);
    }
  }

  protected SimpleAuthzData() {}

  public static SimpleAuthzData ofPermissions(Collection<? extends Permission> permissions) {
    return ofPermissions(permissions, null);
  }

  public static SimpleAuthzData ofPermissions(Collection<? extends Permission> permissions,
      Map<String, ? extends Serializable> attributes) {
    return new SimpleAuthzData(null, permissions, attributes);
  }

  public static SimpleAuthzData ofPermissions(Map<String, ? extends Serializable> attributes,
      String... permissions) {
    return ofPermissions(
        Arrays.stream(permissions).map(SimplePermission::of).collect(Collectors.toSet()),
        attributes);
  }

  public static SimpleAuthzData ofPermissions(String... permissions) {
    return ofPermissions(null, permissions);
  }

  public static SimpleAuthzData ofRoles(Collection<? extends Role> roles) {
    return ofRoles(roles, null);
  }

  public static SimpleAuthzData ofRoles(Collection<? extends Role> roles,
      Map<String, ? extends Serializable> attributes) {
    return new SimpleAuthzData(roles, null, attributes);
  }

  public static SimpleAuthzData ofRoles(Map<String, ? extends Serializable> attributes,
      String... roles) {
    return new SimpleAuthzData(Arrays.stream(roles).map(SimpleRole::of).collect(Collectors.toSet()),
        null, attributes);
  }

  public static SimpleAuthzData ofRoles(String... roles) {
    return ofRoles(null, roles);
  }

  @Override
  public Map<String, ? extends Serializable> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends Permission> getPermissions() {
    return permissions;
  }

  @Override
  public Collection<? extends Role> getRoles() {
    return roles;
  }

}

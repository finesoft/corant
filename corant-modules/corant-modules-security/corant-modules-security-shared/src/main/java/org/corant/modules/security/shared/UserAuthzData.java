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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.modules.security.Permission;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午4:11:00
 *
 */
public class UserAuthzData extends SimpleAuthzData {

  protected Serializable userId;

  public UserAuthzData(Serializable userId) {
    this(userId, (Collection<String>) null, (Collection<String>) null, null);
  }

  public UserAuthzData(Serializable userId, Collection<String> roles,
      Collection<String> permissions) {
    this(userId, roles, permissions, null);
  }

  public UserAuthzData(Serializable userId, Collection<String> roles,
      Collection<String> permissions, Map<String, ? extends Serializable> attributes) {
    super(roles, permissions, attributes);
    this.userId = userId;
  }

  public UserAuthzData(Serializable userId, List<? extends Role> roles,
      List<? extends Permission> permissions) {
    this(userId, roles, permissions, null);
  }

  public UserAuthzData(Serializable userId, List<? extends Role> roles,
      List<? extends Permission> permissions, Map<String, ? extends Serializable> attributes) {
    super(roles, permissions, attributes);
    this.userId = userId;
  }

  protected UserAuthzData() {}

  public static UserAuthzData ofPermissions(Serializable userId,
      List<? extends Permission> permissions) {
    return ofPermissions(userId, permissions, null);
  }

  public static UserAuthzData ofPermissions(Serializable userId,
      List<? extends Permission> permissions, Map<String, ? extends Serializable> attributes) {
    return new UserAuthzData(userId, null, permissions, attributes);
  }

  public static UserAuthzData ofPermissions(Serializable userId,
      Map<String, ? extends Serializable> attributes, String... permissions) {
    return ofPermissions(userId,
        Arrays.stream(permissions).map(SimplePermission::of).collect(Collectors.toList()),
        attributes);
  }

  public static UserAuthzData ofPermissions(Serializable userId, String... permissions) {
    return ofPermissions(userId, null, permissions);
  }

  public static UserAuthzData ofRoles(Serializable userId, List<? extends Role> roles) {
    return ofRoles(userId, roles, null);
  }

  public static UserAuthzData ofRoles(Serializable userId, List<? extends Role> roles,
      Map<String, ? extends Serializable> attributes) {
    return new UserAuthzData(userId, roles, null, attributes);
  }

  public static UserAuthzData ofRoles(Serializable userId,
      Map<String, ? extends Serializable> attributes, String... roles) {
    return ofRoles(userId, Arrays.stream(roles).map(SimpleRole::of).collect(Collectors.toList()),
        attributes);
  }

  public static UserAuthzData ofRoles(Serializable userId, String... roles) {
    return ofRoles(userId, null, roles);
  }

  public Serializable getUserId() {
    return userId;
  }

}

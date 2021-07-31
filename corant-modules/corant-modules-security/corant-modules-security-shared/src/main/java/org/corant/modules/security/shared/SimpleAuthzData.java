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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.corant.modules.security.AuthorizationData;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:17:41
 *
 */
public class SimpleAuthzData implements AuthorizationData {

  protected List<Role> roles;

  public SimpleAuthzData(Collection<String> roles) {
    this.roles = roles.stream().map(SimpleRole::of).collect(Collectors.toUnmodifiableList());
  }

  public SimpleAuthzData(List<SimpleRole> roles) {
    this.roles = Collections.unmodifiableList(newArrayList(roles));
  }

  public SimpleAuthzData(String... roles) {
    this.roles = Arrays.stream(roles).map(SimpleRole::of).collect(Collectors.toUnmodifiableList());
  }

  protected SimpleAuthzData() {}

  public List<String> getRoleNames() {
    return roles.stream().map(Role::getName).collect(Collectors.toList());
  }

  @Override
  public Collection<? extends Role> getRoles() {
    return roles;
  }

}

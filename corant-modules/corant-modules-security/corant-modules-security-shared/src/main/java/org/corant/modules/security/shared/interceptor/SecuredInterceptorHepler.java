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
package org.corant.modules.security.shared.interceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.corant.modules.security.annotation.SecuredMetadata;
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimpleRoles;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午11:48:56
 *
 */
public interface SecuredInterceptorHepler extends Sortable {

  SecuredInterceptorHepler DEFAULT_INST = new SecuredInterceptorHepler() {

    Map<SecuredMetadata, Object> roles = new ConcurrentHashMap<>();
    Map<SecuredMetadata, Object> perms = new ConcurrentHashMap<>();

    @Override
    public Object resolveAllowedPermission(SecuredMetadata meta) {
      return perms.computeIfAbsent(meta, k -> SimplePermissions.of(k.allowed()));
    }

    @Override
    public Object resolveAllowedRole(SecuredMetadata meta) {
      return roles.computeIfAbsent(meta, k -> SimpleRoles.of(k.allowed()));
    }
  };

  default void handleRunAs(String runAs) {
    throw new NotSupportedException(
        "Runas is not currently supported, implementers can implement it themselves");
  }

  Object resolveAllowedPermission(SecuredMetadata meta);

  Object resolveAllowedRole(SecuredMetadata meta);

}

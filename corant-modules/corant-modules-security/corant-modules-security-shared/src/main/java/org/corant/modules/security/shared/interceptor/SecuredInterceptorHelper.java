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
import org.corant.modules.security.annotation.SecuredType;
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
public interface SecuredInterceptorHelper extends Sortable {

  SecuredInterceptorHelper DEFAULT_INST = new SecuredInterceptorHelper() {

    final Map<SecuredMetadata, SecuredInterceptionContext> caches = new ConcurrentHashMap<>();

    @Override
    public SecuredInterceptionContext resolveContext(SecuredMetadata meta) {
      return caches.computeIfAbsent(meta, m -> {
        if (!m.equals(SecuredMetadata.EMPTY_INST)) {
          return new SecuredInterceptionContext(
              meta.type() == SecuredType.ROLE ? SimpleRoles.of(m.allowed())
                  : SimplePermissions.of(m.allowed()),
              meta.type(), meta.runAs(), meta.denyAll());
        }
        return SecuredInterceptionContext.ALLOWED_ALL;
      });
    }
  };

  default void handleRunAs(String runAs) {
    throw new NotSupportedException(
        "Runas is not currently supported, implementers can implement it themselves");
  }

  SecuredInterceptionContext resolveContext(SecuredMetadata meta);

}

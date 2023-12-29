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
package org.corant.modules.security;

import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午11:58:04
 */
public interface AuthorizerCallback extends Sortable {
  /**
   * A callback after access check.
   *
   * @param context the caller context, in general the context is current SecurityContext.
   * @param success true means pass the access check, false means access check failed
   */
  default void postCheckAccess(Object context, boolean success) {}

  /**
   * A callback before access check.
   *
   * @param context the caller context, in general the context is current SecurityContext.
   * @param roleOrPermit the necessary (pre-configured) roles or permissions, used to compute
   *        whether the caller has access privileges with the roles or permissions acquired by the
   *        caller context.
   */
  default void preCheckAccess(Object context, Object roleOrPermit) {}

}

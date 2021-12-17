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
package org.corant.modules.security;

import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-api
 *
 * @author bingo 12:24:41
 *
 */
public interface Authorizer extends Sortable {

  default void checkAccess(Object context, Object roleOrPermit) throws AuthorizationException {
    if (!testAccess(context, roleOrPermit)) {
      throw new AuthorizationException((Object) SecurityMessageCodes.UNAUTHZ_ACCESS);
    }
  }

  boolean testAccess(Object context, Object roleOrPermit);

}

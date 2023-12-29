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

import static org.corant.shared.util.Objects.areEqual;
import java.io.Serializable;
import org.corant.modules.security.Role;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:41:08
 */
public interface IdentifiableRole extends Role {

  Serializable getId();

  @Override
  default boolean implies(Role role) {
    if (!(role instanceof IdentifiableRole)) {
      return false;
    }
    return areEqual(getId(), ((IdentifiableRole) role).getId());
  }

  @Override
  default <T> T unwrap(Class<T> cls) {
    if (IdentifiableRole.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return Role.super.unwrap(cls);
  }

}

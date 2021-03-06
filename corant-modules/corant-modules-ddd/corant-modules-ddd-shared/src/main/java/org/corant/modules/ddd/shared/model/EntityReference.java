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
package org.corant.modules.ddd.shared.model;

import static org.corant.shared.util.Preconditions.requireTrue;
import java.io.Serializable;
import java.util.function.Predicate;
import org.corant.modules.ddd.Entity;
import org.corant.modules.ddd.Reference;

public interface EntityReference<T extends Entity> extends Reference<T> {

  @SuppressWarnings("rawtypes")
  static <T extends EntityReference> T validate(T obj, Object code, Object... parameters) {
    return requireTrue(obj, o -> o != null && o.getId() != null, code, parameters);
  }

  @SuppressWarnings("rawtypes")
  static <T extends EntityReference> T validate(T obj, Predicate<? super T> p, Object code,
      Object... parameters) {
    return requireTrue(obj, o -> o != null && o.getId() != null && p.test(o), code, parameters);
  }

  Serializable getId();
}

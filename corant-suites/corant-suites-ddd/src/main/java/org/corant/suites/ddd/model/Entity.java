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
package org.corant.suites.ddd.model;

import static org.corant.kernel.util.Preconditions.requireTrue;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.function.Predicate;
import javax.persistence.EntityManager;

/**
 * @author bingo 上午12:26:44
 *
 */
public interface Entity extends Serializable {

  Serializable getId();

  public interface EntityIdentifier extends Value {

    Serializable getId();

    Serializable getType();
  }

  @FunctionalInterface
  public interface EntityManagerProvider {
    EntityManager getEntityManager(Annotation qualifier);
  }

  public interface EntityReference<T extends Entity> extends Reference<T> {

    @SuppressWarnings("rawtypes")
    static <T extends EntityReference> T validate(T obj, Object code, Object... parameters) {
      return requireTrue(obj, o -> o != null && o.getId() != null, code, parameters);
    }

    @SuppressWarnings("rawtypes")
    static <T extends EntityReference> T validate(T obj, Predicate<T> p, Object code,
        Object... parameters) {
      return requireTrue(obj, o -> o != null && o.getId() != null && p.test(o), code, parameters);
    }

    Serializable getId();
  }
}

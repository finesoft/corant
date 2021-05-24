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

import static org.corant.modules.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import static org.corant.modules.bundle.GlobalMessageCodes.ERR_PARAM;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.corant.context.Instances;
import org.corant.modules.bundle.exception.GeneralRuntimeException;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.shared.repository.JPARepository;
import org.corant.modules.ddd.shared.repository.JPARepositoryExtension;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午8:39:07
 *
 */
public class Aggregates {

  private Aggregates() {}

  public static <X extends Aggregate> X resolve(Class<X> cls, Serializable id) {
    if (id != null && cls != null) {
      return shouldNotNull(resolveRepository(cls).get(cls, id),
          () -> new GeneralRuntimeException(ERR_PARAM));
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> X resolve(Class<X> cls, String namedQuery,
      Map<Object, Object> params) {
    if (isNotBlank(namedQuery)) {
      List<X> list = resolveRepository(cls).namedQuery(namedQuery).parameters(params).select();
      if (!isEmpty(list)) {
        if (list.size() > 1) {
          throw new GeneralRuntimeException(ERR_OBJ_NON_FUD);
        }
        return list.get(0);
      }
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> X resolve(Class<X> cls, String namedQuery, Object... params) {
    if (isNotBlank(namedQuery)) {
      List<X> list = resolveRepository(cls).namedQuery(namedQuery).parameters(params).select();
      if (!isEmpty(list)) {
        if (list.size() > 1) {
          throw new GeneralRuntimeException(ERR_OBJ_NON_FUD);
        }
        return list.get(0);
      }
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> List<X> select(Class<X> cls, String namedQuery,
      Object... params) {
    return resolveRepository(cls).namedQuery(namedQuery).parameters(params).select();
  }

  public static <X extends Aggregate> Optional<X> tryResolve(Class<X> cls, Serializable id) {
    if (id != null && cls != null) {
      return Optional.ofNullable(resolveRepository(cls).get(cls, id));
    }
    return Optional.empty();
  }

  static JPARepository resolveRepository(Class<?> cls) {
    return Instances.resolve(JPARepository.class,
        Instances.resolve(JPARepositoryExtension.class).resolveQualifiers(cls));
  }
}

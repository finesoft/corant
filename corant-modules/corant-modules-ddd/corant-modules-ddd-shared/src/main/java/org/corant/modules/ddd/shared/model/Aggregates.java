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
import static org.corant.modules.ddd.shared.model.PkgMsgCds.ERR_AGG_RESOLVE_MULTI;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.LockModeType;
import javax.persistence.metamodel.EntityType;
import org.corant.context.Beans;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.shared.repository.JPARepository;
import org.corant.modules.ddd.shared.repository.JPARepositoryExtension;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * A convenient context aggregate instance retrieval class for retrieving aggregate instances
 *
 * @author bingo 下午8:39:07
 *
 */
public class Aggregates {

  static final Map<Class<?>, String> AGG_ID_EXISTS_QLS = new ConcurrentHashMap<>();

  private Aggregates() {}

  public static void clearAggIdExistsSqlCache() {
    AGG_ID_EXISTS_QLS.clear();
  }

  public static <X extends Aggregate> boolean exists(Class<X> cls, Serializable id) {
    if (id != null && cls != null) {
      final String existQl = AGG_ID_EXISTS_QLS.computeIfAbsent(cls, c -> {
        EntityType<X> et = resolveRepository(cls).getEntityManager().getMetamodel().entity(cls);
        String idAttrName = et.getId(et.getIdType().getJavaType()).getName();
        String entityName = getUserClass(cls).getCanonicalName();
        return new StringBuilder(36 + idAttrName.length() + entityName.length()).append("SELECT A.")
            .append(idAttrName).append(" FROM ").append(entityName).append(" A WHERE A.")
            .append(idAttrName).append(" =:id").toString();
      });
      return resolveRepository(cls).query(existQl).parameters(Map.of("id", id)).get() != null;
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> void lock(Aggregate obj, LockModeType lockModeType,
      Object... properties) {
    if (obj != null) {
      if (properties.length > 0) {
        resolveRepository(getUserClass(obj)).lock(obj, lockModeType, mapOf(properties));
      } else {
        resolveRepository(getUserClass(obj)).lock(obj, lockModeType);
      }
    } else {
      throw new GeneralRuntimeException(ERR_PARAM);
    }
  }

  public static <X extends Aggregate> X resolve(Class<X> cls, Serializable id,
      LockModeType lockModeType, Object... properties) {
    if (id != null && cls != null) {
      if (properties.length > 0) {
        return shouldNotNull(resolveRepository(cls).get(cls, id, lockModeType, mapOf(properties)),
            () -> new GeneralRuntimeException(ERR_OBJ_NON_FUD, id));
      } else {
        return shouldNotNull(resolveRepository(cls).get(cls, id, lockModeType),
            () -> new GeneralRuntimeException(ERR_OBJ_NON_FUD, id));
      }
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> X resolve(Class<X> cls, Serializable id,
      Object... properties) {
    if (id != null && cls != null) {
      if (properties.length > 0) {
        return shouldNotNull(resolveRepository(cls).get(cls, id, mapOf(properties)),
            () -> new GeneralRuntimeException(ERR_OBJ_NON_FUD, id));
      } else {
        return shouldNotNull(resolveRepository(cls).get(cls, id),
            () -> new GeneralRuntimeException(ERR_OBJ_NON_FUD, id));
      }
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> X resolve(Class<X> cls, String namedQuery,
      Map<Object, Object> params) {
    if (isNotBlank(namedQuery)) {
      List<X> list = resolveRepository(cls).namedQuery(namedQuery).parameters(params).select();
      if (!isEmpty(list)) {
        if (list.size() > 1) {
          throw new GeneralRuntimeException(ERR_AGG_RESOLVE_MULTI, params);
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
          throw new GeneralRuntimeException(ERR_AGG_RESOLVE_MULTI, params);
        }
        return list.get(0);
      }
    }
    throw new GeneralRuntimeException(ERR_PARAM);
  }

  public static <X extends Aggregate> List<X> select(Class<X> cls, String namedQuery,
      Map<Object, Object> params) {
    return resolveRepository(cls).namedQuery(namedQuery).parameters(params).select();
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
    return Beans.resolve(JPARepository.class,
        Beans.resolve(JPARepositoryExtension.class).resolveQualifiers(cls));
  }
}

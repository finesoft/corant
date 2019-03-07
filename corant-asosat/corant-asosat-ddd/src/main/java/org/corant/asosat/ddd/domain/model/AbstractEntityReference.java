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
package org.corant.asosat.ddd.domain.model;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import javax.enterprise.inject.literal.NamedLiteral;
import org.corant.Corant;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.suites.ddd.model.Entity;
import org.corant.suites.ddd.model.Entity.EntityReference;
import org.corant.suites.ddd.repository.JpaRepository;
import org.corant.suites.ddd.unitwork.JpaPersistenceService;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午1:52:48
 *
 */
public abstract class AbstractEntityReference<T extends Entity> extends AbstractValueObject
    implements EntityReference<T> {

  private static final long serialVersionUID = 1261945123532200005L;

  protected static final Annotation[] DEFAULT_REPO_QLFS = new Annotation[] {NamedLiteral.INSTANCE};

  protected static JpaRepository obtainRepo(Annotation... qualifiers) {
    return Corant.instance().select(JpaRepository.class, qualifiers).get();
  }

  protected static <T> T retrieve(Serializable id, Class<T> cls, Annotation... qualifiers) {
    if (id != null && cls != null) {
      T persistObj = obtainRepo(qualifiers).get(cls, id);
      return persistObj;
    }
    return null;
  }

  protected static <T> T retrieve(String namedQuery, Annotation[] qualifiers, Object... params) {
    if (isNotBlank(namedQuery)) {
      List<T> persistObjs = obtainRepo(qualifiers).select(namedQuery, params);
      if (!isEmpty(persistObjs)) {
        if (persistObjs.size() > 1) {
          throw new GeneralRuntimeException(ERR_OBJ_NON_FUD);
        }
        return persistObjs.get(0);
      }
    }
    return null;
  }

  protected static <T> T retrieve(String namedQuery, Map<Object, Object> params,
      Annotation... qualifiers) {
    if (isNotBlank(namedQuery)) {
      List<T> persistObjs = obtainRepo(qualifiers).select(namedQuery, params);
      if (!isEmpty(persistObjs)) {
        if (persistObjs.size() > 1) {
          throw new GeneralRuntimeException(ERR_OBJ_NON_FUD);
        }
        return persistObjs.get(0);
      }
    }
    return null;
  }

  protected static <T> List<T> retrieveList(String namedQuery, Annotation[] qualifiers,
      Object... params) {
    return obtainRepo(qualifiers).select(namedQuery, params);
  }

  @Override
  public T retrieve() {
    return retrieve(getId(), resolveClass(), this.obtainRepoQualifiers());
  }

  protected Annotation[] obtainRepoQualifiers() {
    Annotation qf = Corant.instance().select(JpaPersistenceService.class).get()
        .getPersistenceUnitQualifier(resolveClass());
    return new Annotation[] {qf};
  }

  @SuppressWarnings("unchecked")
  protected Class<T> resolveClass() {
    Class<?> t = this.getClass();
    do {
      if (t.getGenericSuperclass() instanceof ParameterizedType) {
        Class<T> clz =
            (Class<T>) ((ParameterizedType) t.getGenericSuperclass()).getActualTypeArguments()[0];
        return clz;
      }
    } while ((t = t.getSuperclass()) != null);
    return null;
  }
}

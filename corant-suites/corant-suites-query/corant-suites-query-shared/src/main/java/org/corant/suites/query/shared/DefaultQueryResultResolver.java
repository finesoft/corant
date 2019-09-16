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
package org.corant.suites.query.shared;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.putKeyPathMapValue;
import static org.corant.shared.util.StringUtils.isBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;

@ApplicationScoped
@Alternative
public class DefaultQueryResultResolver implements QueryResultResolver {

  @Inject
  @Any
  protected Instance<ResultHintHandler> resultHintHandlers;

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> resolve(List<?> results, Class<T> resultClass, List<QueryHint> hints,
      QueryParameter parameter) {
    List<T> list = new ArrayList<>();
    if (!isEmpty(results)) {
      resolveResultHints(results, resultClass, hints, parameter);
      if (Map.class.isAssignableFrom(resultClass)) {
        for (Object r : results) {
          list.add((T) r);
        }
      } else {
        for (Object r : results) {
          list.add(r == null ? null
              : Map.class.isAssignableFrom(resultClass) ? (T) r
                  : QueryObjectMapper.OM.convertValue(r, resultClass));
        }
      }
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T resolve(Object result, Class<T> resultClass, List<QueryHint> hints,
      QueryParameter parameter) {
    if (result == null) {
      return null;
    } else {
      resolveResultHints(result, resultClass, hints, parameter);
      return Map.class.isAssignableFrom(resultClass) ? (T) result
          : QueryObjectMapper.OM.convertValue(result, resultClass);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void resolveFetchedResult(Object result, Object fetchedResult, String injectProName) {
    if (isBlank(injectProName)) {
      return;
    }
    if (result instanceof Map) {
      if (injectProName.indexOf('.') != -1) {
        Map<String, Object> mapResult = Map.class.cast(result);
        putKeyPathMapValue(mapResult, injectProName, ".", fetchedResult);
      } else {
        Map.class.cast(result).put(injectProName, fetchedResult);
      }
    } else if (result != null) {
      try {
        BeanUtils.setProperty(result, injectProName, fetchedResult);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }

  @Override
  public void resolveResultHints(Object result, Class<?> resultClass, List<QueryHint> hints,
      QueryParameter parameter) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      hints.forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.canHandle(resultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, parameter, result);
                  exclusive.set(h.exclusive());
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                }
              }
            });
      });
    }
  }
}

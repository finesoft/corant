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

import static org.corant.shared.util.ConversionUtils.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.suites.query.shared.QueryParameter.LIMIT_PARAM_NME;
import static org.corant.suites.query.shared.QueryParameter.OFFSET_PARAM_NME;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.kernel.api.ConversionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;

@ApplicationScoped
// @Alternative
public class DefaultQueryResolver implements QueryResolver {

  @Inject
  @Any
  protected Instance<ResultHintHandler> resultHintHandlers;

  @Inject
  ConversionService conversionService;

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

  @Override
  public QueryParameter resolveQueryParameter(Query query, Object param) {
    QueryParameter queryParameter = DefaultQueryParameter.EMPTY_INST;
    if (param instanceof QueryParameter) {
      queryParameter = (QueryParameter) param;
    } else if (param instanceof Map) {
      Map<?, ?> mp = new HashMap<>((Map<?, ?>) param);
      DefaultQueryParameter qp = new DefaultQueryParameter();
      Optional.ofNullable(mp.remove(LIMIT_PARAM_NME)).ifPresent(x -> qp.limit(toInteger(x, 1)));
      Optional.ofNullable(mp.remove(OFFSET_PARAM_NME)).ifPresent(x -> qp.offset(toInteger(x, 0)));
      queryParameter = qp.criteria(convertParameter(mp, query.getParamConvertSchema()));
    } else if (param != null) {
      queryParameter = new DefaultQueryParameter().criteria(param);
    }
    return queryParameter;
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

  protected Map<String, Object> convertParameter(Map<?, ?> param,
      Map<String, Class<?>> convertSchema) {
    Map<String, Object> convertedParam = new HashMap<>();
    if (param != null) {
      param.forEach((k, v) -> {
        Class<?> cls = null;
        if (convertSchema != null && (cls = convertSchema.get(k)) != null) {
          convertedParam.put(k.toString(), conversionService.convert(v, cls));
        } else if (k != null) {
          convertedParam.put(k.toString(), v);
        }
      });
    }
    return convertedParam;
  }
}

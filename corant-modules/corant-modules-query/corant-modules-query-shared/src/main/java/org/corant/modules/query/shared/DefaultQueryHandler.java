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
package org.corant.modules.query.shared;

import static org.corant.context.Beans.select;
import static org.corant.modules.query.QueryParameter.CONTEXT_NME;
import static org.corant.modules.query.QueryParameter.LIMIT_PARAM_NME;
import static org.corant.modules.query.QueryParameter.OFFSET_PARAM_NME;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.forceCast;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.config.declarative.ConfigInstances;
import org.corant.context.service.ConversionService;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.spi.QueryParameterReviser;
import org.corant.modules.query.spi.ResultHintHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午6:50:41
 *
 */
@ApplicationScoped
// @Alternative
public class DefaultQueryHandler implements QueryHandler {

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<ResultHintHandler> resultHintHandlers;

  @Inject
  protected ConversionService conversionService;

  @Inject
  protected QueryObjectMapper objectMapper;

  protected DefaultQuerierConfig querierConfig = DefaultQuerierConfig.DFLT_INST;

  @Override
  public QueryObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public DefaultQuerierConfig getQuerierConfig() {
    return querierConfig;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T handleResult(Object result, Class<T> resultClass, List<QueryHint> hints,
      QueryParameter parameter) {
    if (result == null) {
      return null;
    } else {
      handleResultHints(result, resultClass, hints, parameter);
      return Map.class.isAssignableFrom(resultClass) ? (T) result
          : convertRecord(result, resultClass);
    }
  }

  @Override
  public void handleResultHints(Object result, Class<?> resultClass, List<QueryHint> hints,
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

  @Override
  public <T> List<T> handleResults(List<Object> results, Class<T> resultClass,
      List<QueryHint> hints, QueryParameter parameter) {
    if (!isEmpty(results)) {
      handleResultHints(results, resultClass, hints, parameter);
      return convertRecords(results, resultClass);
    }
    return forceCast(results);
  }

  @SuppressWarnings("unchecked")
  @Override
  public QueryParameter resolveParameter(Query query, Object param) {
    MutableObject<QueryParameter> resolved = new MutableObject<>();
    if (param instanceof QueryParameter) {
      resolved.set((QueryParameter) param);
    } else if (param instanceof Map) {
      Map<?, ?> mp = new HashMap<>((Map<?, ?>) param);
      DefaultQueryParameter qp = new DefaultQueryParameter();
      Optional.ofNullable(mp.remove(LIMIT_PARAM_NME)).ifPresent(x -> qp.limit(toInteger(x, 1)));
      Optional.ofNullable(mp.remove(OFFSET_PARAM_NME)).ifPresent(x -> qp.offset(toInteger(x, 0)));
      Optional.ofNullable(mp.remove(CONTEXT_NME))
          .ifPresent(x -> qp.context((Map<String, Object>) x));
      Map<String, Class<?>> convertSchema = query == null ? null : query.getParamConvertSchema();
      resolved.set(qp.criteria(convertParameter(mp, convertSchema)));
    } else if (param != null) {
      resolved.set(new DefaultQueryParameter().criteria(param));
    } else {
      resolved.set(new DefaultQueryParameter());
    }
    select(QueryParameterReviser.class).stream().filter(r -> r.canHandle(query))
        .sorted(Sortable::compare).forEach(resolved::apply);
    return resolved.get();
  }

  protected Map<String, Object> convertParameter(Map<?, ?> param,
      Map<String, Class<?>> convertSchema) {
    Map<String, Object> convertedParam = new HashMap<>();
    if (param != null) {
      param.forEach((k, v) -> {
        Class<?> cls;
        if (k != null && convertSchema != null && (cls = convertSchema.get(k)) != null) {
          convertedParam.put(k.toString(), conversionService.convert(v, cls));
        } else if (k != null) {
          convertedParam.put(k.toString(), v);
        }
      });
    }
    return convertedParam;
  }

  /**
   * Convert single record to expected class object.
   *
   * @param <T> the expected type
   * @param record the record to be converted
   * @param expectedClass the excepted class
   */
  protected <T> T convertRecord(Object record, Class<T> expectedClass) {
    return objectMapper.toObject(record, expectedClass);
  }

  /**
   * Convert multiple record to expected list.
   *
   * @param <T> the expected type
   * @param records the record to be converted
   * @param expectedClass the excepted class
   */
  protected <T> List<T> convertRecords(List<Object> records, Class<T> expectedClass) {
    final boolean needConvert = !Map.class.isAssignableFrom(expectedClass);
    if (needConvert) {
      return objectMapper.toObjects(records, expectedClass);
    } else {
      return forceCast(records);
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    querierConfig = ConfigInstances.resolveSingle(DefaultQuerierConfig.class);
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    if (!resultHintHandlers.isUnsatisfied()) {
      resultHintHandlers.stream().forEach(handler -> {
        try {
          handler.close();
        } catch (Exception e) {
          logger.log(Level.WARNING, e,
              () -> String.format("Close result hint handler %s occurred error!",
                  handler.getClass().getName()));
        }
      });
    }
  }
}

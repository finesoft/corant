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
import static org.corant.modules.query.QueryParameter.CTX_QHH_DONT_CONVERT_RESULT;
import static org.corant.modules.query.QueryParameter.CTX_QHH_EXCLUDE_RESULTHINT;
import static org.corant.modules.query.QueryParameter.LIMIT_PARAM_NME;
import static org.corant.modules.query.QueryParameter.OFFSET_PARAM_NME;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapBoolean;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.isSimpleClass;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.matchWildcard;
import static org.corant.shared.util.Strings.split;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.config.Configs;
import org.corant.context.service.ConversionService;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.spi.QueryParameterReviser;
import org.corant.modules.query.spi.ResultHintHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings.WildcardMatcher;

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

  @Override
  public <T> T handleResult(Object result, Query query, QueryParameter parameter) {
    if (result != null) {
      if (!getMapBoolean(parameter.getContext(), CTX_QHH_DONT_CONVERT_RESULT, false)) {
        return forceCast(Map.class.isAssignableFrom(query.getResultClass()) ? result
            : convertRecord(result, query.getResultClass()));
      } else {
        return forceCast(result);
      }
    }
    return null;
  }

  @Override
  public void handleResultHints(Object result, Class<?> originalResultClass, Query query,
      QueryParameter parameter) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      final String exs = getMapString(parameter.getContext(), CTX_QHH_EXCLUDE_RESULTHINT);
      final Predicate<QueryHint> predicate;
      if (isBlank(exs)) {
        predicate = Functions.emptyPredicate(true);
      } else {
        predicate = h -> {
          for (String ex : split(exs, ",", true, true)) {
            if (WildcardMatcher.hasWildcard(ex) && matchWildcard(h.getKey(), false, ex)
                || areEqual(h.getKey(), ex)) {
              return false;
            }
          }
          return true;
        };
      }
      query.getHints().stream().filter(predicate::test).forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.supports(originalResultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, query, parameter, result);
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
  public <T> List<T> handleResults(List<Object> results, Query query, QueryParameter parameter) {
    if (!isEmpty(results)
        && !getMapBoolean(parameter.getContext(), CTX_QHH_DONT_CONVERT_RESULT, false)) {
      return convertRecords(results, forceCast(query.getResultClass()));
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
    select(QueryParameterReviser.class).stream().filter(r -> r.supports(query))
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
   * @param result the result to be converted
   * @param expectedClass the excepted class
   */
  protected <T> T convertRecord(Object result, Class<T> expectedClass) {
    final boolean needConvert = !Map.class.isAssignableFrom(expectedClass);
    if (needConvert) {
      if (isSimpleClass(expectedClass)) {
        if (result == null) {
          return null;
        }
        Object object = ((Map<?, ?>) result).entrySet().iterator().next().getValue();
        if (object != null && isSimpleClass(object.getClass())) {
          return Conversions.toObject(object, expectedClass);
        }
        throw new QueryRuntimeException("Can't support result type from %s to %s conversion.",
            result.getClass(), expectedClass);
      } else {
        return objectMapper.toObject(result, expectedClass);
      }
    } else {
      return forceCast(result);
    }
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
      if (isSimpleClass(expectedClass)) {
        records.replaceAll(e -> {
          if (e == null) {
            return null;
          }
          Map<?, ?> em = (Map<?, ?>) e;
          // FIXME assert or not?
          // shouldBeTrue(em.size() == 1, "Can't support result type from %s to %s conversion.",
          // e.getClass(), expectedClass);
          return Conversions.toObject(em.entrySet().iterator().next().getValue(), expectedClass);
        });
        return forceCast(records);
      } else {
        return objectMapper.toObjects(records, expectedClass);
      }
    } else {
      return forceCast(records);
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    querierConfig = Configs.resolveSingle(DefaultQuerierConfig.class);
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

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

import static java.util.Collections.emptyMap;
import static org.corant.modules.query.QueryParameter.CONTEXT_NME;
import static org.corant.modules.query.QueryParameter.CTX_QHH_DONT_CONVERT_RESULT;
import static org.corant.modules.query.QueryParameter.CTX_QHH_EXCLUDE_RESULT_HINT;
import static org.corant.modules.query.QueryParameter.LIMIT_PARAM_NME;
import static org.corant.modules.query.QueryParameter.OFFSET_PARAM_NME;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapBoolean;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.defaultObject;
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
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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
import org.corant.modules.query.spi.ResultRecordConverter;
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
  @Any
  protected Instance<ResultRecordConverter> resultRecordConverters;

  @Inject
  protected ConversionService conversionService;

  @Inject
  protected QueryObjectMapper objectMapper;

  @Inject
  @Any
  protected Instance<QueryParameterReviser> parameterRevisers;

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
            : convertRecord(query, parameter, result));
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
      final String exs = getMapString(parameter.getContext(), CTX_QHH_EXCLUDE_RESULT_HINT);
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
      query.getHints().stream().filter(predicate).forEach(qh -> {
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
      return convertRecords(query, parameter, results);
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
    parameterRevisers.stream().filter(r -> r.supports(query)).sorted(Sortable::compare)
        .forEach(resolved::apply);
    return resolved.get();
  }

  protected Map<String, Object> convertParameter(Map<?, ?> param,
      Map<String, Class<?>> convertSchema) {
    Map<String, Object> convertedParam = new HashMap<>();
    Map<String, Class<?>> usedConvertSchema = defaultObject(convertSchema, emptyMap());
    if (param != null) {
      param.forEach((k, v) -> {
        if (k != null) {
          Class<?> cls = usedConvertSchema.get(k);
          if (cls != null) {
            convertedParam.put(k.toString(), conversionService.convert(v, cls));
          } else {
            convertedParam.put(k.toString(), v);
          }
        }
      });
    }
    return convertedParam;
  }

  /**
   * Convert a single record to an expected class object.
   *
   * @param <T> the expected type
   * @param query the query
   * @param queryParameter the query parameter
   * @param result the result to be converted
   */
  protected <T> T convertRecord(Query query, QueryParameter queryParameter, Object result) {
    Class<?> expectedClass = query.getResultClass();
    final boolean needConvert = !Map.class.isAssignableFrom(expectedClass);
    if (needConvert) {
      ResultRecordConverter converter = resolveResultRecordConverter(query, queryParameter);
      if (converter != null) {
        return forceCast(converter.convert(query, queryParameter, result));
      } else if (isSimpleClass(expectedClass)) {
        return convertSimpleRecord(result, expectedClass);
      } else {
        return objectMapper.toObject(result, expectedClass);
      }
    } else {
      return forceCast(result);
    }
  }

  /**
   * Convert multiple record to an expected list.
   *
   * @param <T> the expected type
   * @param query the query
   * @param queryParameter the query parameter
   * @param records the record to be converted
   */
  protected <T> List<T> convertRecords(Query query, QueryParameter queryParameter,
      List<Object> records) {
    Class<?> expectedClass = query.getResultClass();
    final boolean needConvert = !Map.class.isAssignableFrom(expectedClass);
    if (needConvert) {
      ResultRecordConverter converter = resolveResultRecordConverter(query, queryParameter);
      if (converter != null) {
        return forceCast(converter.convert(query, queryParameter, records));
      } else if (isSimpleClass(expectedClass)) {
        records.replaceAll(e -> convertSimpleRecord(e, expectedClass));
        return forceCast(records);
      } else {
        return objectMapper.toObjects(records, expectedClass);
      }
    } else {
      return forceCast(records);
    }
  }

  protected <T> T convertSimpleRecord(Object result, Class<?> expectedClass) {
    if (result == null) {
      return null;
    }
    if (result instanceof Map) {
      Object object = ((Map<?, ?>) result).entrySet().iterator().next().getValue();
      if (object == null) {
        return null;
      } else {
        return forceCast(Conversions.toObject(object, expectedClass));
      }
    } else if (expectedClass.isInstance(result)) {
      return forceCast(result);
    }
    throw new QueryRuntimeException("Can't support result type from %s to %s conversion.",
        result.getClass(), expectedClass);
  }

  @PostConstruct
  protected void onPostConstruct() {
    querierConfig = Configs.resolveSingle(DefaultQuerierConfig.class);
  }

  protected ResultRecordConverter resolveResultRecordConverter(Query query,
      QueryParameter queryParameter) {
    if (!resultRecordConverters.isUnsatisfied()) {
      return resultRecordConverters.stream().filter(rrc -> rrc.supports(query, queryParameter))
          .max(ResultRecordConverter::compare).orElse(null);
    }
    return null;
  }

}

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
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.suites.query.shared.QueryParameter.LIMIT_PARAM_NME;
import static org.corant.suites.query.shared.QueryParameter.OFFSET_PARAM_NME;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.kernel.api.ConversionService;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.suites.query.shared.mapping.Query;

@ApplicationScoped
// @Alternative
public class DefaultQueryParameterResolver implements QueryParameterResolver {

  @Inject
  ConversionService conversionService;

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery query,
      QueryParameter parentQueryparameter) {
    return new DefaultQueryParameter().context(parentQueryparameter.getContext())
        .criteria(resolveFetchQueryCriteria(result, query, extractCriterias(parentQueryparameter)));
  }

  @SuppressWarnings("unchecked")
  @Override
  public QueryParameter resolveQueryParameter(Query query, Object param) {
    QueryParameter queryParameter = DefaultQueryParameter.EMPTY_INST;
    if (param instanceof QueryParameter) {
      queryParameter = QueryParameter.class.cast(param);
    } else if (param instanceof Map) {
      Map<?, ?> mp = new HashMap<>(Map.class.cast(param));
      DefaultQueryParameter qp = new DefaultQueryParameter();
      Optional.ofNullable(mp.remove(LIMIT_PARAM_NME)).ifPresent(x -> qp.limit(toInteger(x, 1)));
      Optional.ofNullable(mp.remove(OFFSET_PARAM_NME)).ifPresent(x -> qp.offset(toInteger(x, 0)));
      queryParameter = qp.criteria(convertParameter(mp, query.getParamConvertSchema()));
    } else if (param != null) {
      queryParameter = new DefaultQueryParameter().criteria(param);
    }
    return queryParameter;
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

  @SuppressWarnings("unchecked")
  protected Map<String, Object> extractCriterias(QueryParameter parameter) {
    Map<String, Object> map = new HashMap<>();
    if (parameter != null) {
      Object criteria = parameter.getCriteria();
      if (criteria instanceof Map) {
        Map.class.cast(criteria).forEach((k, v) -> {
          map.put(asDefaultString(k), v);
        });
      } else if (criteria != null) {
        QueryObjectMapper.OM.convertValue(criteria, Map.class).forEach((k, v) -> {
          map.put(asDefaultString(k), v);
        });
      }
    }
    return map;
  }

  protected Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery,
      Map<String, Object> criteria) {
    Map<String, Object> fetchCriteria = new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      if (parameter.getSource() == FetchQueryParameterSource.C) {
        fetchCriteria.put(parameter.getName(), parameter.getValue());
      } else if (parameter.getSource() == FetchQueryParameterSource.P) {
        fetchCriteria.put(parameter.getName(), criteria.get(parameter.getSourceName()));
      } else if (result != null) {
        String parameterName = parameter.getName();
        String sourceName = parameter.getSourceName();
        if (result instanceof Map) {
          if (sourceName.indexOf('.') != -1) {
            List<Object> values = new ArrayList<>();
            QueryUtils.extractResult(result, sourceName, true, values);
            Object value = values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
            fetchCriteria.put(parameterName, value);
          } else {
            fetchCriteria.put(parameterName, Map.class.cast(result).get(sourceName));
          }
        } else {
          try {
            fetchCriteria.put(parameterName, BeanUtils.getProperty(result, sourceName));
          } catch (Exception e) {
            throw new QueryRuntimeException(e,
                "Can not extract value from query result for resolve fetch query [%s] parameter!",
                fetchQuery.getReferenceQuery());
          }
        }
      }
    }
    return fetchCriteria;
  }

}

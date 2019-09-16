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

import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.ConversionUtils.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.split;
import static org.corant.suites.query.shared.QueryService.LIMIT_PARAM_NME;
import static org.corant.suites.query.shared.QueryService.OFFSET_PARAM_NME;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.kernel.api.ConversionService;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.suites.query.shared.mapping.Query;

@ApplicationScoped
@Alternative
public class DefaultQueryParameterResolver implements QueryParameterResolver {

  @Inject
  ConversionService conversionService;

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> extractCriterias(QueryParameter parameter) {
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

  @Override
  public Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery query,
      QueryParameter parentQueryparameter) {
    return resolveFetchQueryCriteria(result, query, extractCriterias(parentQueryparameter));
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
      Optional.of(mp.remove(LIMIT_PARAM_NME)).ifPresent(x -> qp.limit(toInteger(x, -1)));
      Optional.of(mp.remove(OFFSET_PARAM_NME)).ifPresent(x -> qp.offset(toInteger(x, -1)));
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

  protected void extractResult(Object result, String paths, boolean flatList, List<Object> list) {
    extractResult(result, split(paths, ".", true, false), flatList, list);
  }

  protected void extractResult(Object result, String[] paths, boolean flatList, List<Object> list) {
    if (!interruptExtract(result, paths, flatList, list)) {
      if (result instanceof Map) {
        String path = paths[0];
        Object next = Map.class.cast(result).get(path);
        if (next != null) {
          extractResult(next, Arrays.copyOfRange(paths, 1, paths.length), flatList, list);
        }
      } else if (result instanceof Iterable) {
        for (Object next : Iterable.class.cast(result)) {
          if (next != null) {
            extractResult(next, paths, flatList, list);
          }
        }
      } else if (result != null) {
        extractResult(listOf((Object[]) result), paths, flatList, list);// may be array
      }
    }
  }

  protected boolean interruptExtract(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (isEmpty(paths)) {
      if (result instanceof Iterable && flatList) {
        listOf((Iterable<?>) result).forEach(list::add);
      } else {
        list.add(result);
      }
      return true;
    }
    return false;
  }

  Map<String, Object> resolveFetchQueryCriteria(Object obj, FetchQuery fetchQuery,
      Map<String, Object> param) {
    Map<String, Object> pmToUse = new HashMap<>();
    fetchQuery.getParameters().forEach(p -> {
      if (p.getSource() == FetchQueryParameterSource.C) {
        pmToUse.put(p.getName(), p.getValue());
      } else if (p.getSource() == FetchQueryParameterSource.P) {
        pmToUse.put(p.getName(), param.get(p.getSourceName()));
      } else if (obj != null) {
        if (obj instanceof Map) {
          String paramName = p.getName();
          String srcName = p.getSourceName();
          if (srcName.indexOf('.') != -1) {
            List<Object> srcVal = new ArrayList<>();
            extractResult(obj, srcName, true, srcVal);
            pmToUse.put(paramName,
                srcVal.isEmpty() ? null : srcVal.size() == 1 ? srcVal.get(0) : srcVal);
          } else {
            pmToUse.put(paramName, Map.class.cast(obj).get(srcName));
          }
        } else {
          try {
            pmToUse.put(p.getName(), BeanUtils.getProperty(obj, p.getSourceName()));
          } catch (Exception e) {
            throw new QueryRuntimeException(e,
                "Can not extract value from query result for fetch query [%s] param!",
                fetchQuery.getReferenceQuery());
          }
        }
      }
    });
    return pmToUse;
  }

}

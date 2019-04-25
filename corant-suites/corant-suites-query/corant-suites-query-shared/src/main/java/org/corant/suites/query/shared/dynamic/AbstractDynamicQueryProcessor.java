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
package org.corant.suites.query.shared.dynamic;

import static org.corant.shared.util.Empties.isEmpty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * @author bingo 上午9:54:00
 *
 */
public abstract class AbstractDynamicQueryProcessor<Q, P, E>
    implements DynamicQueryProcessor<Q, E> {

  protected final String queryName;
  protected final Map<String, Class<?>> paramConvertSchema;
  protected final long cachedTimestemp;
  protected final Class<?> resultClass;
  protected final List<FetchQuery> fetchQueries;
  protected final ConversionService conversionService;
  protected final List<QueryHint> hints = new ArrayList<>();

  protected AbstractDynamicQueryProcessor(Query query, ConversionService conversionService) {
    if (query == null || conversionService == null) {
      throw new QueryRuntimeException(
          "Can not initialize freemarker query template from null query param!");
    }
    this.conversionService = conversionService;
    this.fetchQueries = Collections.unmodifiableList(query.getFetchQueries());
    this.queryName = query.getName();
    this.resultClass = query.getResultClass() == null ? Map.class : query.getResultClass();
    this.paramConvertSchema = Collections.unmodifiableMap(query.getParamMappings().entrySet()
        .stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getType())));
    if (!isEmpty(query.getHints())) {
      query.getHints().forEach(hints::add);
    }
    this.cachedTimestemp = Instant.now().toEpochMilli();
  }

  @Override
  public long getCachedTimestemp() {
    return cachedTimestemp;
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  @Override
  public List<QueryHint> getHints() {
    return hints;
  }

  @Override
  public Map<String, Class<?>> getParamConvertSchema() {
    return paramConvertSchema;
  }

  @Override
  public String getQueryName() {
    return queryName;
  }

  @Override
  public Class<?> getResultClass() {
    return resultClass;
  }

  /**
   * Convert parameter to use.
   *
   * @param param
   * @return convertParameter
   */
  protected Map<String, Object> convertParameter(Map<String, Object> param) {
    Map<String, Object> convertedParam = new HashMap<>();
    if (param != null) {
      convertedParam.putAll(param);
      getParamConvertSchema().forEach((pn, pc) -> {
        if (convertedParam.containsKey(pn)) {
          convertedParam.put(pn, conversionService.convert(param.get(pn), pc));
        }
      });
    }
    return convertedParam;
  }

}

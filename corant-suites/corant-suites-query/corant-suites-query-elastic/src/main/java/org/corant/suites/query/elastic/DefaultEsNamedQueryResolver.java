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
package org.corant.suites.query.elastic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.mapping.QueryMappingService;
import org.corant.suites.query.shared.spi.ParamReviser;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultEsNamedQueryResolver implements
    EsInLineNamedQueryResolver<String, Map<String, Object>, String, FetchQuery, QueryHint> {

  final Map<String, DefaultEsNamedQueryProcessor> processors = new ConcurrentHashMap<>();

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected ConversionService conversionService;

  @Inject
  @Any
  Instance<ParamReviser> paramRevisers;

  @Override
  public DefaultEsNamedQuerier resolve(String key, Map<String, Object> param) {
    DefaultEsNamedQueryProcessor processor = processors.computeIfAbsent(key, this::buildProcessor);
    handleParamHints(processor, param);
    return processor.process(param);
  }

  protected DefaultEsNamedQueryProcessor buildProcessor(String key) {
    Query query = mappingService.getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not found Query for key %s", key);
    }
    return new DefaultEsNamedQueryProcessor(query, conversionService);
  }

  protected void handleParamHints(DefaultEsNamedQueryProcessor tpl, Map<String, Object> param) {
    if (!paramRevisers.isUnsatisfied()) {
      paramRevisers.stream().sorted().forEach(pr -> pr.accept(tpl.getQueryName(), param));
    }
  }

}
/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.query.dynamic.template;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.mapping.FetchQuery;
import org.corant.suites.query.mapping.Query;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * asosat-query
 *
 * @author bingo 下午3:50:40
 *
 */
public abstract class FreemarkerDynamicQueryTpl<T, P> implements DynamicQueryTpl<T> {

  static final Configuration FM_CFG = new Configuration(Configuration.VERSION_2_3_28);

  final String queryName;
  final Template template;
  final Map<String, Class<?>> paramConvertSchema;
  final long cachedTimestemp;
  final Class<?> resultClass;
  final List<FetchQuery> fetchQueries;
  final ConversionService conversionService;

  public FreemarkerDynamicQueryTpl(Query query, ConversionService conversionService) {
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
    this.cachedTimestemp = Instant.now().toEpochMilli();
    try {
      this.template = new Template(this.queryName, query.getScript(), FM_CFG);
    } catch (IOException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public long getCachedTimestemp() {
    return this.cachedTimestemp;
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return this.fetchQueries;
  }

  @Override
  public Map<String, Class<?>> getParamConvertSchema() {
    return this.paramConvertSchema;
  }

  @Override
  public String getQueryName() {
    return this.queryName;
  }

  @Override
  public Class<?> getResultClass() {
    return this.resultClass;
  }

  @Override
  public Template getTemplate() {
    return this.template;
  }

  @Override
  public T process(Map<String, Object> param) {
    Map<String, Object> clonedParam = new HashMap<>();
    if (param != null) {
      clonedParam.putAll(param);
    }
    DynamicQueryTplResolver<P> qtmm = this.getTemplateMethodModel();
    this.preProcess(clonedParam, qtmm);
    T result = this.doProcess(clonedParam);
    this.postProcess(result, qtmm);
    return result;
  }

  protected abstract T doProcess(Map<String, Object> param);

  protected abstract DynamicQueryTplResolver<P> getTemplateMethodModel();

  protected void postProcess(T result, DynamicQueryTplResolver<P> qtmm) {
    // NOOP
  }

  protected void preProcess(Map<String, Object> param, DynamicQueryTplResolver<P> qtmm) {
    this.getParamConvertSchema().forEach((pn, pc) -> {
      if (param.containsKey(pn)) {
        param.put(pn, this.conversionService.convert(param.get(pn), pc));
      }
    });
    param.put(qtmm.getType().name(), qtmm);
  }
}

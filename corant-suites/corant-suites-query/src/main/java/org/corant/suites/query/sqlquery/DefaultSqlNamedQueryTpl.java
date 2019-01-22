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
package org.corant.suites.query.sqlquery;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.dynamic.template.DynamicQueryTplResolver;
import org.corant.suites.query.dynamic.template.FreemarkerDynamicQueryTpl;
import org.corant.suites.query.mapping.Query;
import freemarker.template.TemplateException;

/**
 * asosat-query
 *
 * @author bingo 下午7:46:22
 *
 */
public class DefaultSqlNamedQueryTpl
    extends FreemarkerDynamicQueryTpl<DefaultSqlNamedQuerier, Object[]> {

  /**
   * @param query
   */
  public DefaultSqlNamedQueryTpl(Query query, ConversionService conversionService) {
    super(query, conversionService);
  }

  @Override
  public DefaultSqlNamedQuerier doProcess(Map<String, Object> param) {
    try (StringWriter sw = new StringWriter()) {
      Map<String, Object> paramClone = new HashMap<>(param);
      getTemplate().process(paramClone, sw);
      return new DefaultSqlNamedQuerier(sw.toString(), new Object[0], getResultClass(),
          getFetchQueries());// FIXME process parameter
    } catch (TemplateException | IOException | NullPointerException e) {
      throw new QueryRuntimeException("Freemarker process stringTemplate is error", e);
    }

  }

  @Override
  protected DynamicQueryTplResolver<Object[]> getTemplateMethodModel() {
    return new DefaultSqlNamedQueryTplResolver();
  }

  @Override
  protected void postProcess(DefaultSqlNamedQuerier result,
      DynamicQueryTplResolver<Object[]> qtmm) {
    result.withParam(qtmm.getParameters());
    super.postProcess(result, qtmm);
  }

  @Override
  protected void preProcess(Map<String, Object> param, DynamicQueryTplResolver<Object[]> qtmm) {
    super.preProcess(param, qtmm);
  }

}

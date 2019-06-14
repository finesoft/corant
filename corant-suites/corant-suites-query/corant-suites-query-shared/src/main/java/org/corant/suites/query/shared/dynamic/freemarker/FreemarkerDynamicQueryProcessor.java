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
package org.corant.suites.query.shared.dynamic.freemarker;

import static org.corant.shared.util.ObjectUtils.forceCast;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.corant.kernel.service.ConversionService;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQueryProcessor;
import org.corant.suites.query.shared.mapping.Query;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:00:50
 *
 */
public abstract class FreemarkerDynamicQueryProcessor<Q, P>
    extends AbstractDynamicQueryProcessor<Q, P, Template> {

  static final Configuration FM_CFG = new Configuration(Configuration.VERSION_2_3_28);

  protected final Template execution;

  /**
   * @param query
   * @param conversionService
   */
  protected FreemarkerDynamicQueryProcessor(Query query, ConversionService conversionService) {
    super(query, conversionService);
    try {
      execution = new Template(queryName, query.getScript(), FM_CFG);
    } catch (IOException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the query template [%s].", queryName);
    }
  }

  @Override
  public Template getExecution() {
    return execution;
  }

  /**
   * Use parameters to process query script.
   */
  @Override
  public Q process(Map<String, Object> param) {
    Map<String, Object> useParam = convertParameter(param);// convert parameter
    this.preProcess(useParam);
    Pair<String, P> executeResult = execute(useParam);
    Q result = this.doProcess(executeResult.getKey(), executeResult.getValue());
    this.postProcess(result, executeResult.getValue());
    return result;
  }

  /**
   *
   * @param script
   * @param param
   * @return doProcess
   */
  protected abstract Q doProcess(String script, P param);

  protected Pair<String, P> execute(Map<String, Object> param) {
    try (StringWriter sw = new StringWriter()) {
      DynamicQueryTplMmResolver<P> tmm = handleTemplateMethodModel(param);
      getExecution().process(param, sw);
      return Pair.of(sw.toString(), tmm.getParameters());
    } catch (IOException | TemplateException e) {
      throw new QueryRuntimeException(e, "Freemarker process stringTemplate occurred and error");
    }
  }

  protected DynamicQueryTplMmResolver<P> handleTemplateMethodModel(Map<String, Object> useParam) {
    return new DynamicQueryTplMmResolver<P>() {

      @SuppressWarnings("rawtypes")
      @Override
      public Object exec(List arguments) throws TemplateModelException {
        return arguments;
      }

      @Override
      public P getParameters() {
        return forceCast(useParam);
      }

      @Override
      public QueryTemplateMethodModelType getType() {
        return null;
      }

    };
  }

  protected void postProcess(Q result, P param) {}

  protected void preProcess(Map<String, Object> param) {}

}

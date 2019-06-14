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
package org.corant.suites.query.jpql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQueryProcessor;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines.ScriptFunction;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:46:22
 *
 */
public class JpqlNamedQueryJsProcessor
    extends AbstractDynamicQueryProcessor<DefaultJpqlNamedQuerier, Object[], ScriptFunction> {

  final ScriptFunction execution;

  public JpqlNamedQueryJsProcessor(Query query, ConversionService conversionService) {
    super(query, conversionService);
    execution = NashornScriptEngines.compileFunction(query.getScript(), "p", "up");
  }

  @Override
  public ScriptFunction getExecution() {
    return execution;
  }

  /**
   * Generate SQL script with placeholder, and converted the parameter to appropriate type.
   */
  @Override
  public DefaultJpqlNamedQuerier process(Map<String, Object> param) {
    Map<String, Object> convertedParam = convertParameter(param);// convert parameter
    List<Object> useParam = new ArrayList<>();
    Object script = getExecution().apply(new Object[] {convertedParam, useParam});
    return new DefaultJpqlNamedQuerier(script.toString(),
        useParam.toArray(new Object[useParam.size()]), getResultClass(), getHints(),
        getProperties());
  }

}

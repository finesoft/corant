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
package org.corant.asosat.ddd.application.query;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.Map;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.asosat.ddd.application.query.ResultMapperHintHandler.ResultMapperResolver;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.query.mapping.QueryHint;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:10:26
 *
 */
@SuppressWarnings("restriction")
@ApplicationScoped
@InfrastructureServices
public class NashornResultMapperResolver implements ResultMapperResolver {

  public static final String DFLT_SCRIPT_ENGINE = "Oracle Nashorn";

  private static final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

  @Override
  public boolean accept(QueryHint qh) {
    return qh != null && isNotBlank(qh.getScript())
        && (isEmpty(qh.getParameters())
            || isEmpty(qh.getParameters(ResultMapperHintHandler.HNIT_SCRIPT_ENGINE))
            || defaultString(
                qh.getParameters(ResultMapperHintHandler.HNIT_SCRIPT_ENGINE).get(0).getValue(),
                DFLT_SCRIPT_ENGINE).equals(DFLT_SCRIPT_ENGINE));
  }

  @Override
  public Consumer<Map<?, ?>> resolve(QueryHint qh) throws Exception {
    // -doe Dump a stack trace on errors.
    // --global-per-engine Use single Global instance per script engine instance
    final ScriptEngine scriptEngine =
        factory.getScriptEngine(new String[] {"-doe", "--global-per-engine"});
    final CompiledScript compiled = ((Compilable) scriptEngine).compile(qh.getScript());
    return (m) -> {
      Bindings bindings = new SimpleBindings();
      bindings.put("parameter", m);
      try {
        compiled.eval(bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      }
    };
  }

}

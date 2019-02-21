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
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.corant.suites.query.mapping.QueryHint;
import org.corant.suites.query.spi.ResultHintHandler;
import jdk.nashorn.api.scripting.NashornScriptEngine;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
@ApplicationServices
public class ProgrammableMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-mapper";
  public static final String HNIT_SCRIPT_ENGINE = "script-engine";
  public static final String DFLT_SCRIPT_ENGINE = "nashorn";

  final ScriptEngineManager engineManager = new ScriptEngineManager();
  // ScriptEngine and CompiledScript are not thread safe, we use state-less so it thread safe.
  final Map<String, ScriptEngine> engines = new ConcurrentHashMap<>();
  final Map<String, CompiledScript> scripts = new ConcurrentHashMap<>();

  @Override
  public boolean canHandle(QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), HINT_NAME) && isNotBlank(hint.getScript());
  }

  @Override
  public void handle(QueryHint qh, Object result) {
    final ScriptEngine se = resolveEngine(qh);
    final CompiledScript cs = resolveScript(qh, se);
    if (cs != null) {
      Bindings bindings = se.createBindings();
      try {
        cs.eval(bindings);
      } catch (ScriptException e) {
      }
    }
  }

  protected ScriptEngine resolveEngine(QueryHint qh) {
    String name = DFLT_SCRIPT_ENGINE;
    if (!isEmpty(qh.getParameters(HNIT_SCRIPT_ENGINE))) {
      if (isNotBlank(qh.getParameters(HNIT_SCRIPT_ENGINE).get(0).getValue())) {
        name = qh.getParameters(HNIT_SCRIPT_ENGINE).get(0).getValue();
      }
    }
    return engines.computeIfAbsent(name, (en) -> engineManager.getEngineByName(en));
  }

  protected CompiledScript resolveScript(QueryHint qh, ScriptEngine engine) {
    String script = qh.getScript();
    return scripts.computeIfAbsent(script, (s) -> {
      try {
        if (engine instanceof NashornScriptEngine) {
          return NashornScriptEngine.class.cast(engine).compile(s);
        }
        return null;
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  @PostConstruct
  void onPostConstruct() {}

  @PreDestroy
  void onPreDestroy() {}
}

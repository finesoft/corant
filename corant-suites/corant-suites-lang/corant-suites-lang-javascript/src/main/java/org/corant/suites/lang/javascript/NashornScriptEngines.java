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
package org.corant.suites.lang.javascript;

import static org.corant.shared.util.StringUtils.isBlank;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.shared.exception.CorantRuntimeException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:02:11
 *
 */
@SuppressWarnings("restriction")
public class NashornScriptEngines {

  public static final NashornScriptEngineFactory NASHORN_ENGINE_FACTORY =
      new NashornScriptEngineFactory();

  // -doe Dump a stack trace on errors.
  // --global-per-engine Use single Global instance per script engine instance
  public static final ScriptEngine ENGINE =
      NASHORN_ENGINE_FACTORY.getScriptEngine("-doe", "--global-per-engine");

  public static Consumer<Object[]> compileConsumer(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    try {
      final CompiledScript compiled = ((Compilable) ENGINE).compile(funcScript);
      return pns -> {
        Bindings bindings = new SimpleBindings();
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        try {
          compiled.eval(bindings);
        } catch (ScriptException e) {
          throw new CorantRuntimeException(e);
        }
      };
    } catch (ScriptException e) {
      throw new CorantRuntimeException(e);
    }

  }

  public static Function<Object[], Object> compileFunction(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    try {
      final CompiledScript compiled = ((Compilable) ENGINE).compile(funcScript);
      return pns -> {
        Bindings bindings = new SimpleBindings();
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        try {
          return compiled.eval(bindings);
        } catch (ScriptException e) {
          throw new CorantRuntimeException(e);
        }
      };
    } catch (ScriptException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

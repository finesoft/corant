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
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.shared.exception.CorantRuntimeException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * corant-suites-query
 *
 * NOTE: The nashorn script engine is not thread safe, so we use thread local to hold it.
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
  public static final ThreadLocal<ScriptEngine> ENGINES = ThreadLocal
      .withInitial(() -> NASHORN_ENGINE_FACTORY.getScriptEngine("-doe", "--global-per-engine"));

  public static Consumer<Object[]> createConsumer(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        ENGINES.get().eval(funcScript, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }

  public static Function<Object[], Object> createFunction(String funcScript, String... paraNames) {
    if (isBlank(funcScript)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        return ENGINES.get().eval(funcScript, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }
}

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
package org.corant.modules.lang.javascript;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.isBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * corant-modules-lang-javascript
 *
 * NOTE: The nashorn script engine is not thread safe, so we use thread local to hold it.
 *
 * @author bingo 下午7:02:11
 *
 */
@SuppressWarnings({"removal"})
public class NashornScriptEngines {

  public static final NashornScriptEngineFactory NASHORN_ENGINE_FACTORY =
      new NashornScriptEngineFactory();

  public static final ThreadLocal<ScriptEngine> ENGINES =
      ThreadLocal.withInitial(NashornScriptEngines::createEngine);

  public static final ThreadLocal<Map<Object, Consumer<Object[]>>> CONSUMERS =
      ThreadLocal.withInitial(HashMap::new);

  public static final ThreadLocal<Map<Object, Function<Object[], Object>>> FUNCTIONS =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * Complie a thread local consumer with specified id and script and parameter names that are used
   * in script, all complied consumers are not thread safe, means that don't share the complied
   * consumer in multi threads. the script was complied only once in every thread. we don't use
   * script as id, because the script may have very large size.
   *
   * <pre>
   * NOTE: Usually, the passed in script should be a IIFE (Immediately Invoked Function Expression).
   * Example:
   *    (function(p){
   *         //do somthing;
   *     })(p);
   * </pre>
   *
   * @see <a href="https://en.wikipedia.org/wiki/Immediately_invoked_function_expression">IIFE</a>
   *
   * @param id the specified id, client use this id to retrive the appropriate consumer
   * @param scriptAndParamNames the script and parameter names use for compling.
   * @return the complied consumer
   */
  public static Consumer<Object[]> complieConsumer(Object id,
      Supplier<Pair<String, String[]>> scriptAndParamNames) {
    return CONSUMERS.get().computeIfAbsent(id, k -> {
      try {
        final Pair<String, String[]> snp = shouldNotNull(scriptAndParamNames.get());
        final Compilable se = (Compilable) createEngine();
        final CompiledScript cs = se.compile(shouldNotBlank(snp.getKey()));
        return pns -> {
          Bindings bindings = new SimpleBindings();
          try {
            for (int i = 0; i < pns.length; i++) {
              bindings.put(snp.getValue()[i], pns[i]);
            }
            cs.eval(bindings);
          } catch (ScriptException e) {
            throw new CorantRuntimeException(e);
          } finally {
            bindings.clear();
          }
        };
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  /**
   * Complie a thread local function with specified id and script and parameter names that are used
   * in script, all complied functions are not thread safe, means that don't share the complied
   * function in multi threads. the script was complied only once in every thread. we don't use
   * script as id, because the script may have very large size
   *
   * <pre>
   * NOTE: Usually, the passed in script should be a IIFE (Immediately Invoked Function Expression).
   * Example:
   *    (function(p){
   *         //do somthing;
   *         return true;
   *     })(p);
   * </pre>
   *
   * @see <a href="https://en.wikipedia.org/wiki/Immediately_invoked_function_expression">IIFE</a>
   * @param id the specified id, client use this id to retrive the appropriate function
   * @param scriptAndParamNames the script and parameter names use for compling.
   * @return the complied function
   */
  public static Function<Object[], Object> complieFunction(Object id,
      Supplier<Pair<String, String[]>> scriptAndParamNames) {
    return FUNCTIONS.get().computeIfAbsent(id, k -> {
      try {
        final Pair<String, String[]> snp = scriptAndParamNames.get();
        final Compilable se = (Compilable) createEngine();
        final CompiledScript cs = se.compile(snp.getKey());
        return pns -> {
          Bindings bindings = new SimpleBindings();
          try {
            for (int i = 0; i < pns.length; i++) {
              bindings.put(snp.getValue()[i], pns[i]);
            }
            return cs.eval(bindings);
          } catch (ScriptException e) {
            throw new CorantRuntimeException(e);
          } finally {
            bindings.clear();
          }
        };
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  /**
   * Create consumer with script and parameter names use thread local script engine. The complied
   * consumer is thread safe.
   *
   * @param script the script use for compling.
   * @param paraNames the script parameter names.
   * @return the complied consumer
   */
  public static Consumer<Object[]> createConsumer(String script, String... paraNames) {
    if (isBlank(script)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        ENGINES.get().eval(script, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }

  /**
   * Create the jsr223 java script engine. Defaults: dump a stack trace on errors, use single Global
   * instance per script engine instance.
   *
   * @return the Nashorn script engine
   */
  public static ScriptEngine createEngine() {
    // -doe Dump a stack trace on errors.
    // --global-per-engine Use single Global instance per script engine instance
    return createEngine("-doe", "--global-per-engine", "--no-deprecation-warning");
  }

  /**
   * Create the jsr223 java script engine whit args.
   *
   * @param args
   * @return the Nashorn script engine
   */
  public static ScriptEngine createEngine(String... args) {
    return NASHORN_ENGINE_FACTORY.getScriptEngine(args);
  }

  /**
   * Create function with script and parameter names use thread local script engine. The complied
   * function is thread safe.
   *
   * @param script the script use for compling.
   * @param paraNames the script parameter names.
   * @return the complied function
   */
  public static Function<Object[], Object> createFunction(String script, String... paraNames) {
    if (isBlank(script)) {
      return null;
    }
    return pns -> {
      Bindings bindings = new SimpleBindings();
      try {
        for (int i = 0; i < pns.length; i++) {
          bindings.put(paraNames[i], pns[i]);
        }
        return ENGINES.get().eval(script, bindings);
      } catch (ScriptException e) {
        throw new CorantRuntimeException(e);
      } finally {
        bindings.clear();
      }
    };
  }
}

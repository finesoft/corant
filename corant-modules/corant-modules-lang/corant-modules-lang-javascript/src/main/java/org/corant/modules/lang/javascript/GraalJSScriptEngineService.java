/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
import org.corant.modules.lang.shared.ScriptEngineService;
import org.corant.modules.lang.shared.ScriptLang;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.service.RequiredClassPresent;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

/**
 * corant-modules-lang-javascript
 *
 * @author bingo 上午10:31:12
 */
@RequiredClassPresent("com.oracle.truffle.js.scriptengine.GraalJSScriptEngine")
public class GraalJSScriptEngineService implements ScriptEngineService {

  public final ThreadLocal<ScriptEngine> ENGINES = ThreadLocal.withInitial(this::createEngine);

  public final ThreadLocal<Map<Object, Consumer<Object[]>>> CONSUMERS =
      ThreadLocal.withInitial(HashMap::new);

  public final ThreadLocal<Map<Object, Function<Object[], Object>>> FUNCTIONS =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * Compile a thread local consumer with specified id and script and parameter names that are used
   * in script, all complied consumers are not thread safe, means that don't share the complied
   * consumer in multi threads. the script was complied only once in every thread. we don't use
   * script as id, because the script may have very large size.
   *
   * <pre>
   * NOTE: Usually, the passed in script should be a IIFE (Immediately Invoked Function Expression).
   * Example:
   *    (function(p){
   *         //do something;
   *     })(p);
   * </pre>
   *
   * @see <a href="https://en.wikipedia.org/wiki/Immediately_invoked_function_expression">IIFE</a>
   *
   * @param id the specified id, client use this id to retrieve the appropriate consumer
   * @param scriptAndParamNames the script and parameter names use for compiling.
   * @return the complied consumer
   */
  public Consumer<Object[]> compileConsumer(Object id,
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
   * Compile a thread local function with specified id and script and parameter names that are used
   * in script, all complied functions are not thread safe, means that don't share the complied
   * function in multi threads. the script was complied only once in every thread. we don't use
   * script as id, because the script may have very large size
   *
   * <pre>
   * NOTE: Usually, the passed in script should be a IIFE (Immediately Invoked Function Expression).
   * Example:
   *    (function(p){
   *         //do something;
   *         return true;
   *     })(p);
   * </pre>
   *
   * @see <a href="https://en.wikipedia.org/wiki/Immediately_invoked_function_expression">IIFE</a>
   * @param id the specified id, client use this id to retrieve the appropriate function
   * @param scriptAndParamNames the script and parameter names use for compiling.
   * @return the complied function
   */
  public Function<Object[], Object> compileFunction(Object id,
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

  @Override
  public Consumer<Object[]> createConsumer(String script, String... paraNames) {
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

  @Override
  public ScriptEngine createEngine(String... args) {
    return GraalJSScriptEngine.create(null,
        Context.newBuilder("js").allowAllAccess(true).allowExperimentalOptions(true)
            .allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true)
            .option("js.nashorn-compat", "true"));
  }

  @Override
  public Function<Object[], Object> createFunction(String script, String... paraNames) {
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

  @Override
  public boolean supports(ScriptLang lang) {
    return ScriptLang.Javascript.equals(lang);
  }

}

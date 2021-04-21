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
package org.corant.modules.query.shared;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.modules.lang.javascript.NashornScriptEngines;
import org.corant.modules.lang.kotlin.KotlinScriptEngines;
import org.corant.modules.query.shared.mapping.FetchQuery;
import org.corant.modules.query.shared.mapping.QueryHint;
import org.corant.modules.query.shared.mapping.Script;
import org.corant.modules.query.shared.mapping.Script.ScriptType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午10:42:18
 *
 */
public class QueryScriptEngines {

  public static final String RESULT_FUNC_PARAMETER_NAME = "r";
  public static final String FETCHED_RESULT_FUNC_PARAMETER_NAME = "fr";
  public static final String RESULTS_FUNC_PARAMETER_NAME = "rs";
  public static final String FETCHED_RESULTS_FUNC_PARAMETER_NAME = "frs";
  public static final String PARAMETER_FUNC_PARAMETER_NAME = "p";

  static final Logger logger = Logger.getLogger(QueryScriptEngines.class.getName());

  static final ThreadLocal<Map<Object, Consumer<Object[]>>> CONSUMERS =
      ThreadLocal.withInitial(HashMap::new);
  static final ThreadLocal<Map<Object, Function<Object[], Object>>> FUNCTIONS =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @param fetchQuery
   * @return resolveFetchInjections
   */
  public static Function<Object[], Object> resolveFetchInjections(FetchQuery fetchQuery) {
    if (fetchQuery.getInjectionScript().isValid()) {
      return complieFunction(fetchQuery.getInjectionScript().getId(),
          () -> Pair.of(fetchQuery.getInjectionScript(),
              new String[] {RESULTS_FUNC_PARAMETER_NAME, FETCHED_RESULTS_FUNC_PARAMETER_NAME}));
    } else {
      return null;
    }
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @param fetchQuery
   * @return resolveFetchPredicates
   */
  public static Function<Object[], Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    if (fetchQuery.getPredicateScript().isValid()) {
      return complieFunction(fetchQuery.getPredicateScript().getId(),
          () -> Pair.of(fetchQuery.getPredicateScript(),
              new String[] {PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME}));
    } else {
      return null;
    }
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @param queryHint
   * @return resolveQueryHintResultScriptMappers
   */
  public static Consumer<Object[]> resolveQueryHintResultScriptMappers(QueryHint queryHint) {
    if (queryHint != null && queryHint.getScript().isValid()) {
      return complieConsumer(queryHint.getScript().getId(), () -> Pair.of(queryHint.getScript(),
          new String[] {PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME}));
    } else {
      return null;
    }
  }

  static Consumer<Object[]> complieConsumer(Object id, Supplier<Pair<Script, String[]>> supplier) {
    return CONSUMERS.get().computeIfAbsent(id, k -> {
      try {
        logger.fine(() -> String.format(
            "Compile the query consumer script, id is %s, the thread name is %s id is %s",
            id.toString(), Thread.currentThread().getName(), Thread.currentThread().getId()));
        final Pair<Script, String[]> snp = shouldNotNull(shouldNotNull(supplier).get());
        final Script script = shouldNotNull(snp.getKey());
        final Compilable se = getCompilable(script.getType());
        final CompiledScript cs = se.compile(shouldNotBlank(script.getCode()));
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

  static Function<Object[], Object> complieFunction(Object id,
      Supplier<Pair<Script, String[]>> supplier) {
    return FUNCTIONS.get().computeIfAbsent(id, k -> {
      try {
        logger.fine(() -> String.format(
            "Compile the query function script, id is %s, the thread name is %s id is %s",
            id.toString(), Thread.currentThread().getName(), Thread.currentThread().getId()));
        final Pair<Script, String[]> snp = shouldNotNull(shouldNotNull(supplier).get());
        final Script script = shouldNotNull(snp.getKey());
        final Compilable se = getCompilable(script.getType());
        final CompiledScript cs = se.compile(shouldNotBlank(script.getCode()));
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

  static Compilable getCompilable(ScriptType type) {
    if (type.equals(ScriptType.JS)) {
      return (Compilable) NashornScriptEngines.createEngine();
    } else if (type.equals(ScriptType.KT)) {
      return (Compilable) KotlinScriptEngines.createEngine();
    } else {
      throw new NotSupportedException(
          "Can't not support script engine for %s, currently we only support using javascript / kotlin as fetch query script.",
          type);
    }
  }
}

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
package org.corant.modules.query.shared;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor.AbstractScriptProcessor;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:34:03
 */
public abstract class AbstractCompilableScriptProcessor extends AbstractScriptProcessor {

  @Override
  public Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getInjectionScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      if (USING_INTEGRITY_FUNCTIONS) {
        return compileFunction(script, PARAMETER_FUNC_PARAMETER_NAME, RESULTS_FUNC_PARAMETER_NAME,
            FETCH_QUERY_FUNC_PARAMETER_NAME, FETCHED_RESULTS_FUNC_PARAMETER_NAME);
      } else {
        return compileFunction(script, null, RESULTS_FUNC_PARAMETER_NAME, null,
            FETCHED_RESULTS_FUNC_PARAMETER_NAME);
      }
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveFetchParameter(FetchQueryParameter parameter) {
    final Script script = parameter.getScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return compileFunction(script, PARAMETER_FUNC_PARAMETER_NAME, RESULTS_FUNC_PARAMETER_NAME);
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getPredicateScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return compileFunction(fetchQuery.getPredicateScript(), PARAMETER_FUNC_PARAMETER_NAME,
          RESULT_FUNC_PARAMETER_NAME);
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveQueryHintResultScriptMappers(
      QueryHint queryHint) {
    final Script script = queryHint.getScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return compileFunction(script, PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME);
    }
    return null;
  }

  protected Function<ParameterAndResult, Object> compileFunction(Script script,
      String parameterPName, String resultPName) {
    return getParamResultFunctions().computeIfAbsent(script.getId(), k -> {
      try {
        logger.fine(() -> format("Compile query script [id:%s], current thread [name:%s, id:%s]",
            script.getId(), Thread.currentThread().getName(), Thread.currentThread().getId()));
        final Compilable se = getCompilable(script.getType());
        final CompiledScript cs = se.compile(shouldNotBlank(script.getCode()));
        return pns -> {
          Bindings bindings = new SimpleBindings();
          try {
            bindings.put(parameterPName, pns.parameter);
            bindings.put(resultPName, pns.result);
            return cs.eval(bindings);
          } catch (ScriptException e) {
            throw new QueryRuntimeException(e);
          } finally {
            bindings.clear();
          }
        };
      } catch (ScriptException e) {
        throw new QueryRuntimeException(e);
      }
    });
  }

  protected Function<ParameterAndResultPair, Object> compileFunction(Script script,
      String parameterPName, String parentResultPName, String fetchQueryPName,
      String fetchResultPName) {
    return getParamResultPairFunctions().computeIfAbsent(script.getId(), k -> {
      try {
        logger.fine(() -> format("Compile query script [id:%s], current thread [name:%s, id:%s]",
            script.getId(), Thread.currentThread().getName(), Thread.currentThread().getId()));
        final Compilable se = getCompilable(script.getType());
        final CompiledScript cs = se.compile(shouldNotBlank(script.getCode()));
        return pns -> {
          Bindings bindings = new SimpleBindings();
          try {
            if (parameterPName != null) {
              bindings.put(parameterPName, pns.parameter);
            }
            bindings.put(parentResultPName, pns.parentResult);
            if (fetchQueryPName != null) {
              bindings.put(fetchQueryPName, pns.fetchQuery);
            }
            bindings.put(fetchResultPName, pns.fetchedResult);
            return cs.eval(bindings);
          } catch (ScriptException e) {
            throw new QueryRuntimeException(e);
          } finally {
            bindings.clear();
          }
        };
      } catch (ScriptException e) {
        throw new QueryRuntimeException(e);
      }
    });
  }

  protected abstract Compilable getCompilable(ScriptType type);

  protected abstract ThreadLocalExecution<Object, Function<ParameterAndResult, Object>> getParamResultFunctions();

  protected abstract ThreadLocalExecution<Object, Function<ParameterAndResultPair, Object>> getParamResultPairFunctions();

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午3:11:18
   *
   */
  public static class ThreadLocalExecution<K, V> {
    long initializedVersion;
    Map<K, V> executions = new HashMap<>();

    public void clear() {
      executions.clear();
    }

    public V computeIfAbsent(K id, Function<? super K, ? extends V> mappingFunction) {
      final long cv = QueryMappingService.getInitializedVersion();
      if (initializedVersion < cv) {
        initializedVersion = cv;
        executions.clear();
        logger.info(() -> format(
            "Clean thread local script executions cache, current initialized version: %s",
            initializedVersion));
      }
      return executions.computeIfAbsent(id, mappingFunction);
    }
  }
}

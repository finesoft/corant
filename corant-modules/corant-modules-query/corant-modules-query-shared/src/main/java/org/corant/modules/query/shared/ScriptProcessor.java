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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.spi.ResultScriptMapperHintHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:13:08
 *
 */
public interface ScriptProcessor extends Sortable {

  String RESULT_FUNC_PARAMETER_NAME = "r";
  String RESULTS_FUNC_PARAMETER_NAME = "rs";
  String FETCHED_RESULT_FUNC_PARAMETER_NAME = "fr";
  String FETCHED_RESULTS_FUNC_PARAMETER_NAME = "frs";
  String PARAMETER_FUNC_PARAMETER_NAME = "p";

  /**
   * Return an executable function converted from the injection script in fetch query. This function
   * can be used by the {@link FetchQueryHandler} to process the fetch result set.
   *
   * @param fetchQuery the fetch query that contains injection script
   * @return an injection function
   */
  Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery);

  /**
   * Return an executable function converted from the processing script in the fetch query
   * parameter. This function can be used by the {@link FetchQueryHandler} to process the fetch
   * query parameters.
   *
   * @param parameter the fetch query parameter that contains script
   * @return a parameter processing function
   */
  default Function<ParameterAndResult, Object> resolveFetchParameter(
      FetchQueryParameter parameter) {
    throw new NotSupportedException();
  }

  /**
   * Return a predicate function converted from the predicate script in fetch query. This function
   * can be used by the {@link FetchQueryHandler} to pre-process the fetch query.
   *
   * @param fetchQuery the fetch query that contains predicate script
   * @return a predicate function
   */
  Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery);

  /**
   * Return a mapper function converted from the query hint. The function can be used by
   * {@link ResultScriptMapperHintHandler} to process the query result.
   *
   * @param queryHint the query hint contains mapper script;
   * @return a mapper function
   */
  default Function<ParameterAndResult, Object> resolveQueryHintResultScriptMappers(
      QueryHint queryHint) {
    throw new NotSupportedException();
  }

  /**
   * Returns whether the processor can support the given script.
   *
   * @param script the script used to test whether this processor can process
   */
  boolean supports(Script script);

  /**
   *
   * corant-modules-query-shared
   *
   * @author bingo 下午2:34:03
   *
   */
  abstract class CompilableScriptProcessor implements ScriptProcessor {

    static final Logger logger = Logger.getLogger(CompilableScriptProcessor.class.getName());

    protected static final ThreadLocal<Map<Object, Function<ParameterAndResult, Object>>> PARAM_RESULT_FUNCTIONS =
        ThreadLocal.withInitial(HashMap::new);

    protected static final ThreadLocal<Map<Object, Function<ParameterAndResultPair, Object>>> PARAM_RESULT_PAIR_FUNCTIONS =
        ThreadLocal.withInitial(HashMap::new);

    @Override
    public Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery) {
      final Script script = fetchQuery.getInjectionScript();
      if (script.isValid()) {
        shouldBeTrue(supports(script));
        return compileFunction(script, null, RESULTS_FUNC_PARAMETER_NAME,
            FETCHED_RESULTS_FUNC_PARAMETER_NAME);
      }
      return null;
    }

    @Override
    public Function<ParameterAndResult, Object> resolveFetchParameter(
        FetchQueryParameter parameter) {
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
      return PARAM_RESULT_FUNCTIONS.get().computeIfAbsent(script.getId(), k -> {
        try {
          logger.fine(() -> String.format(
              "Compile the query consumer script, id is %s, the thread name is %s id is %s",
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

    protected Function<ParameterAndResultPair, Object> compileFunction(Script script,
        String parameterPName, String parentResultPName, String fetchResultPName) {
      return PARAM_RESULT_PAIR_FUNCTIONS.get().computeIfAbsent(script.getId(), k -> {
        try {
          logger.fine(() -> String.format(
              "Compile the query consumer script, id is %s, the thread name is %s id is %s",
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
              bindings.put(fetchResultPName, pns.fetchedResult);
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

    protected abstract Compilable getCompilable(ScriptType type);
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午2:15:04
   *
   */
  class ParameterAndResult {
    public final QueryParameter parameter;
    public final Object result;

    public ParameterAndResult(QueryParameter parameter, Object result) {
      this.parameter = parameter;
      this.result = result;
    }
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午2:15:07
   *
   */
  class ParameterAndResultPair {
    public final QueryParameter parameter;
    public final Object parentResult;
    public final Object fetchedResult;

    public ParameterAndResultPair(QueryParameter parameter, Object parentResult,
        Object fetchedResult) {
      this.parameter = parameter;
      this.parentResult = parentResult;
      this.fetchedResult = fetchedResult;
    }
  }
}

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

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.modules.lang.javascript.NashornScriptEngines;
import org.corant.modules.lang.kotlin.KotlinScriptEngines;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.spi.FetchQueryParameterResolver;
import org.corant.modules.query.spi.FetchQueryPredicate;
import org.corant.modules.query.spi.FetchQueryResultInjector;
import org.corant.modules.query.spi.ResultHintResolver;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午10:42:18
 *
 */
public class QueryScriptEngines {

  public static final String RESULT_FUNC_PARAMETER_NAME = "r";
  public static final String RESULTS_FUNC_PARAMETER_NAME = "rs";
  public static final String FETCHED_RESULTS_FUNC_PARAMETER_NAME = "frs";
  public static final String PARAMETER_FUNC_PARAMETER_NAME = "p";

  static final Logger logger = Logger.getLogger(QueryScriptEngines.class.getName());

  static final ThreadLocal<Map<Object, Function<ParameterAndResult, Object>>> PARAM_RESULT_FUNCTIONS =
      ThreadLocal.withInitial(HashMap::new);

  static final ThreadLocal<Map<Object, Function<ParameterAndResultPair, Object>>> PARAM_RESULT_PAIR_FUNCTIONS =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @param fetchQuery
   * @return resolveFetchInjections
   */
  public static Function<ParameterAndResultPair, Object> resolveFetchInjections(
      FetchQuery fetchQuery) {
    final Script script = fetchQuery.getInjectionScript();
    if (script.isValid()) {
      switch (script.getType()) {
        case KT:
        case JS:
          return complieFunction(script, null, RESULTS_FUNC_PARAMETER_NAME,
              FETCHED_RESULTS_FUNC_PARAMETER_NAME);
        case CDI:
          return p -> {
            resolve(FetchQueryResultInjector.class, NamedLiteral.of(script.getCode()))
                .inject(p.parameter, p.parentResult, p.fetchedResult);
            return null;
          };
        default:
          throw new NotSupportedException();
      }
    } else {
      return null;
    }
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @param fetchQuery
   * @return resolveFetchInjections
   */
  public static Function<ParameterAndResult, Object> resolveFetchParameter(
      FetchQueryParameter parameter) {
    final Script script = parameter.getScript();
    if (script.isValid()) {
      switch (script.getType()) {
        case KT:
        case JS:
          return complieFunction(script, PARAMETER_FUNC_PARAMETER_NAME,
              RESULTS_FUNC_PARAMETER_NAME);
        case CDI:
          return p -> resolve(FetchQueryParameterResolver.class, NamedLiteral.of(script.getCode()))
              .resolve(p.parameter, p.result);
        default:
          throw new NotSupportedException();
      }
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
  public static Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getPredicateScript();
    if (script.isValid()) {
      switch (script.getType()) {
        case KT:
        case JS:
          return complieFunction(fetchQuery.getPredicateScript(), PARAMETER_FUNC_PARAMETER_NAME,
              RESULT_FUNC_PARAMETER_NAME);
        case CDI:
          return p -> resolve(FetchQueryPredicate.class, NamedLiteral.of(script.getCode()))
              .test(p.parameter, p.result);
        default:
          throw new NotSupportedException();
      }
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
  public static Function<ParameterAndResult, Object> resolveQueryHintResultScriptMappers(
      QueryHint queryHint) {
    final Script script;
    if (queryHint != null && (script = queryHint.getScript()).isValid()) {
      switch (script.getType()) {
        case KT:
        case JS:
          return complieFunction(script, PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME);
        case CDI:
          return p -> resolve(ResultHintResolver.class, NamedLiteral.of(script.getCode()))
              .resolve(p.parameter, p.result);
        default:
          throw new NotSupportedException();
      }
    } else {
      return null;
    }
  }

  static Function<ParameterAndResult, Object> complieFunction(Script script, String parameterPName,
      String resultPName) {
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

  static Function<ParameterAndResultPair, Object> complieFunction(Script script,
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

  public static class ParameterAndResult {
    public final QueryParameter parameter;
    public final Object result;

    public ParameterAndResult(QueryParameter parameter, Object result) {
      this.parameter = parameter;
      this.result = result;
    }
  }

  public static class ParameterAndResultPair {
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

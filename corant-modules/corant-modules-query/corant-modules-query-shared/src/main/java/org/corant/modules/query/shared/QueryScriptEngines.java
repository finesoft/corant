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

import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.shared.ScriptProcessor.ParameterAndResult;
import org.corant.modules.query.shared.ScriptProcessor.ParameterAndResultPair;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午10:42:18
 *
 */
@Singleton
public class QueryScriptEngines {

  static final Logger logger = Logger.getLogger(QueryScriptEngines.class.getName());

  @Inject
  protected Instance<ScriptProcessor> processors;

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @see ScriptProcessor#resolveFetchInjections(FetchQuery)
   */
  public Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getInjectionScript();
    final ScriptProcessor processor =
        processors.stream().filter(p -> p.supports(script)).min(Sortable::compare).orElse(null);
    return processor != null ? processor.resolveFetchInjections(fetchQuery) : null;
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @see ScriptProcessor#resolveFetchParameter(FetchQueryParameter)
   */
  public Function<ParameterAndResult, Object> resolveFetchParameter(FetchQueryParameter parameter) {
    final ScriptProcessor processor = processors.stream()
        .filter(p -> p.supports(parameter.getScript())).min(Sortable::compare).orElse(null);
    return processor != null ? processor.resolveFetchParameter(parameter) : null;
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @see ScriptProcessor#resolveFetchPredicates(FetchQuery)
   */
  public Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    final ScriptProcessor processor =
        processors.stream().filter(p -> p.supports(fetchQuery.getPredicateScript()))
            .min(Sortable::compare).orElse(null);
    return processor != null ? processor.resolveFetchPredicates(fetchQuery) : null;
  }

  /**
   * NOTE: Don't share the complied function in multi threads.
   *
   * @see ScriptProcessor#resolveQueryHintResultScriptMappers(QueryHint)
   */
  public Function<ParameterAndResult, Object> resolveQueryHintResultScriptMappers(
      QueryHint queryHint) {
    if (queryHint != null && queryHint.getScript().isValid()) {
      final ScriptProcessor processor = processors.stream()
          .filter(p -> p.supports(queryHint.getScript())).min(Sortable::compare).orElse(null);
      return processor != null ? processor.resolveQueryHintResultScriptMappers(queryHint) : null;
    } else {
      return null;
    }
  }

}

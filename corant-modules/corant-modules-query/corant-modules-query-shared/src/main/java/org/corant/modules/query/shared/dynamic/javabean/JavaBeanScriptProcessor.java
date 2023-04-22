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
package org.corant.modules.query.shared.dynamic.javabean;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.util.Collection;
import java.util.function.Function;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.ScriptProcessor;
import org.corant.modules.query.spi.FetchQueryParameterResolver;
import org.corant.modules.query.spi.FetchQueryPredicate;
import org.corant.modules.query.spi.FetchQueryResultInjector;
import org.corant.modules.query.spi.ResultHintResolver;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:55:59
 *
 */
@Singleton
public class JavaBeanScriptProcessor implements ScriptProcessor {

  @Inject
  @Any
  protected Instance<FetchQueryParameterResolver> fetchQueryParameterResolvers;

  @Inject
  @Any
  protected Instance<FetchQueryPredicate> fetchQueryPredicates;

  @Inject
  @Any
  protected Instance<FetchQueryResultInjector> fetchQueryResultInjectors;

  @Inject
  @Any
  protected Instance<ResultHintResolver> resultHintResolvers;

  @Override
  public void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion) {
    // No op~
  }

  @Override
  public Function<ParameterAndResultPair, Object> resolveFetchInjections(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getInjectionScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return p -> {
        fetchQueryResultInjectors.select(NamedLiteral.of(script.getCode())).get()
            .inject(p.parameter, p.parentResult, p.fetchedResult);
        return null;
      };
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveFetchParameter(FetchQueryParameter parameter) {
    final Script script = parameter.getScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return p -> fetchQueryParameterResolvers.select(NamedLiteral.of(script.getCode())).get()
          .resolve(p.parameter, p.result);
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveFetchPredicates(FetchQuery fetchQuery) {
    final Script script = fetchQuery.getPredicateScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return p -> fetchQueryPredicates.select(NamedLiteral.of(script.getCode())).get()
          .test(p.parameter, p.result);
    }
    return null;
  }

  @Override
  public Function<ParameterAndResult, Object> resolveQueryHintResultScriptMappers(
      QueryHint queryHint) {
    final Script script = queryHint.getScript();
    if (script.isValid()) {
      shouldBeTrue(supports(script));
      return p -> resultHintResolvers.select(NamedLiteral.of(script.getCode())).get()
          .resolve(p.parameter, p.result);
    }
    return null;
  }

  @Override
  public boolean supports(Script script) {
    return script != null && script.getType() == ScriptType.CDI;
  }

}

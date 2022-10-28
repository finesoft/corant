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

import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.shared.QueryMappingService.AfterQueryMappingInitializedHandler;
import org.corant.modules.query.shared.spi.ResultScriptMapperHintHandler;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:13:08
 *
 */
public interface ScriptProcessor extends Sortable, AfterQueryMappingInitializedHandler {

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
   * corant-modules-query-shared
   *
   * @author bingo 下午2:42:21
   *
   */
  abstract class AbstractScriptProcessor implements ScriptProcessor {

    protected final static Logger logger = Logger.getLogger(ScriptProcessor.class.getName());

    @Override
    public void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion) {
      resolveAll(queries, initializedVersion);
    }

    protected int resolveAll(Collection<Query> queries, long initializedVersion) {
      int cs = 0;
      for (Query query : queries) {
        List<FetchQuery> fqs = query.getFetchQueries();
        if (isNotEmpty(fqs)) {
          for (FetchQuery fq : fqs) {
            if (fq.getInjectionScript() != null && supports(fq.getInjectionScript())) {
              try {
                resolveFetchInjections(fq);
                cs++;
              } catch (Exception ex) {
                throw new QueryRuntimeException(ex,
                    "Resolve fetch query [%s -> %s] injection script occurred error!",
                    query.getVersionedName(), fq.getReferenceQuery().getVersionedName());
              }
            }
            if (fq.getParameters() != null) {
              for (FetchQueryParameter fqp : fq.getParameters()) {
                if (fqp.getScript() != null && supports(fqp.getScript())) {
                  try {
                    resolveFetchParameter(fqp);
                    cs++;
                  } catch (Exception ex) {
                    throw new QueryRuntimeException(ex,
                        "Resolve fetch query [%s -> %s] parameter script [%s] occurred error!",
                        query.getVersionedName(), fq.getReferenceQuery().getVersionedName(),
                        fqp.getName());
                  }
                }
              }
            }
            if (fq.getPredicateScript() != null && supports(fq.getPredicateScript())) {
              try {
                resolveFetchPredicates(fq);
                cs++;
              } catch (Exception ex) {
                throw new QueryRuntimeException(ex,
                    "Resolve fetch query [%s -> %s] predication occurred error!",
                    query.getVersionedName(), fq.getReferenceQuery().getVersionedName());
              }
            }
          }
        }
      }
      return cs;
    }
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

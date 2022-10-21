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
package org.corant.modules.query.shared.dynamic.javascript;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.script.Compilable;
import org.corant.modules.lang.javascript.NashornScriptEngines;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Script;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.AbstractCompilableScriptProcessor;
import org.corant.modules.query.shared.cdi.QueryExtension;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午2:11:37
 *
 */
@Singleton
public class JavaScriptProcessor extends AbstractCompilableScriptProcessor {

  static final Logger logger = Logger.getLogger(JavaScriptProcessor.class.getName());

  protected static final ThreadLocal<ThreadLocalExecution<Object, Function<ParameterAndResult, Object>>> PARAM_RESULT_FUNCTIONS =
      ThreadLocal.withInitial(ThreadLocalExecution::new);

  protected static final ThreadLocal<ThreadLocalExecution<Object, Function<ParameterAndResultPair, Object>>> PARAM_RESULT_PAIR_FUNCTIONS =
      ThreadLocal.withInitial(ThreadLocalExecution::new);

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion) {
    // FIXME TODO the script was cached in thread local
    PARAM_RESULT_FUNCTIONS.get().clear();
    PARAM_RESULT_PAIR_FUNCTIONS.get().clear();
    if (QueryExtension.verifyDeployment) {
      logger.info("Start javascript query scripts precompiling.");
      int cs = 0;
      for (Query query : queries) {
        List<FetchQuery> fqs = query.getFetchQueries();
        if (isNotEmpty(fqs)) {
          for (FetchQuery fq : fqs) {
            if (fq.getInjectionScript() != null && supports(fq.getInjectionScript())) {
              resolveFetchInjections(fq);
              cs++;
            }
            if (fq.getParameters() != null) {
              for (FetchQueryParameter fqp : fq.getParameters()) {
                if (fqp.getScript() != null && supports(fqp.getScript())) {
                  resolveFetchParameter(fqp);
                  cs++;
                }
              }
            }
            if (fq.getPredicateScript() != null && supports(fq.getPredicateScript())) {
              resolveFetchPredicates(fq);
              cs++;
            }
          }
        }
      }
      logger.info("Complete " + cs + " javascript query scripts precompiling.");
    }
  }

  @Override
  public boolean supports(Script script) {
    return script != null && script.getType() == ScriptType.JS;
  }

  @Override
  protected Compilable getCompilable(ScriptType type) {
    return (Compilable) NashornScriptEngines.createEngine();
  }

  @Override
  protected ThreadLocalExecution<Object, Function<ParameterAndResult, Object>> getParamResultFunctions() {
    return PARAM_RESULT_FUNCTIONS.get();
  }

  @Override
  protected ThreadLocalExecution<Object, Function<ParameterAndResultPair, Object>> getParamResultPairFunctions() {
    return PARAM_RESULT_PAIR_FUNCTIONS.get();
  }
}

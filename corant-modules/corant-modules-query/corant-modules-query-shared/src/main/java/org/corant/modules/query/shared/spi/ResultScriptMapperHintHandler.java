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
package org.corant.modules.query.shared.spi;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.shared.QueryScriptEngines;
import org.corant.modules.query.shared.ScriptProcessor.ParameterAndResult;
import org.corant.modules.query.spi.ResultHintHandler;

/**
 * corant-modules-query-shared
 *
 * <p>
 * The result script mapper hints, use script to intervene the result.
 * <ul>
 * <li>The key is 'result-script-mapper'</li>
 * <li>In the current implementation, we recommend the use of stateless functions, which take two
 * arguments. The first argument 'p' is used to accept the query parameter, usually a Map structure,
 * and the second argument 'r' is the row data of the result set, usually a Map structure.</li>
 * <li>The script engine of the current implementation is 'Oracle Nashorn' that supports java script
 * language. We use CompiledScript to improve performance, which means you have to pay attention to
 * writing scripts, preferably stateless functions, that can be problematic in multi-threaded
 * conditions if global variables are involved.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Use case:
 *
 * <pre>
 * &lt;query name="GeneralCriterias.get" result-class="java.util.Map"&gt;
 *       &lt;script&gt;
 *           &lt;![CDATA[
 *               SELECT id,enum FROM Table
 *           ]]&gt;
 *       &lt;/script&gt;
 *       &lt;hint key="result-script-mapper"&gt;
 *           &lt;script&gt;
 *               &lt;![CDATA[
 *                   (function(p,r){
 *                       //p is query parameter usually is a Map, r is the record of result.
 *                       r.putAll(p);
 *                   })(p,r);
 *               ]]&gt;
 *           &lt;/script&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 * </pre>
 *
 *
 * @see ResultBeanMapperHintHandler
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
public class ResultScriptMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-script-mapper";

  protected final Set<String> brokens = new CopyOnWriteArraySet<>();// static?

  @Inject
  protected Logger logger;

  @Inject
  protected QueryScriptEngines scriptEngines;

  @SuppressWarnings("rawtypes")
  @Override
  public void handle(QueryHint qh, Query query, Object parameter, Object result) throws Exception {
    Function<ParameterAndResult, Object> func;
    if (brokens.contains(qh.getId()) || (func = resolve(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      func.apply(new ParameterAndResult((QueryParameter) parameter, result));
    } else {
      List<?> list = null;
      if (result instanceof Forwarding) {
        list = ((Forwarding) result).getResults();
      } else if (result instanceof List) {
        list = (List) result;
      } else if (result instanceof Paging) {
        list = ((Paging) result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            func.apply(new ParameterAndResult((QueryParameter) parameter, item));
          }
        }
      }
    }
  }

  @Override
  public boolean supports(Class<?> resultClass, QueryHint hint) {
    return hint != null && areEqual(hint.getKey(), HINT_NAME)
        && isNotBlank(hint.getScript().getCode());
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    brokens.clear();
    logger.fine(() -> "Clear result script mapper hint handler caches.");
  }

  protected Function<ParameterAndResult, Object> resolve(QueryHint qh) {
    Function<ParameterAndResult, Object> func =
        scriptEngines.resolveQueryHintResultScriptMappers(qh);
    if (func == null) {
      brokens.add(qh.getId());
    }
    return func;
  }

}

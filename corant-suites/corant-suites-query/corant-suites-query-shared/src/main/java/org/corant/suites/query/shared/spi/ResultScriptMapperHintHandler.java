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
package org.corant.suites.query.shared.spi;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.QueryScriptEngines;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * <p>
 * The result script mapper hints, use script to intervene the result.
 * <li>The key is 'result-script-mapper'</li>
 * <li>In the current implementation, we recommend the use of stateless functions, which take two
 * arguments. The first argument 'p' is used to accept the query parameter, usually a Map structure,
 * and the second argument 'r' is the row data of the result set, usually a Map structure.</li>
 * <li>The script engine of the current implementation is 'Oracle Nashorn' that supports java script
 * language. We use CompiledScript to improve performance, which means you have to pay attention to
 * writing scripts, preferably stateless functions, that can be problematic in multi-threaded
 * conditions if global variables are involved.</li>
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
 * </p>
 *
 * @see ResultMapperResolver
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

  @Override
  public boolean canHandle(Class<?> resultClass, QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), HINT_NAME)
        && isNotBlank(hint.getScript().getCode());
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void handle(QueryHint qh, Object parameter, Object result) throws Exception {
    Consumer<Object[]> func = null;
    if (brokens.contains(qh.getId()) || (func = resolve(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      func.accept(new Object[] {parameter, (Map) result});
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
            func.accept(new Object[] {parameter, (Map) item});
          }
        }
      }
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    brokens.clear();
    logger.fine(() -> "Clear result script mapper hint handler caches.");
  }

  protected Consumer<Object[]> resolve(QueryHint qh) {
    Consumer<Object[]> func = QueryScriptEngines.resolveQueryHintResultScriptMappers(qh);
    if (func == null) {
      brokens.add(qh.getId());
    }
    return func;
  }

}

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
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.script.ScriptEngineManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.Query.ForwardList;
import org.corant.suites.query.shared.Query.PagedList;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines.ScriptConsumer;
import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * <p>
 * The result script mapper hints, use script to intervene the result.
 * <li>The key is 'result-script-mapper'</li>
 * <li>The value of the parameter that named 'script-engine' is the script engine name that will be
 * use for execute the script.</li>
 * <li>In the current implementation, we recommend the use of stateless functions, which take two
 * arguments. The first argument 'p' is used to accept the query parameter, usually a Map structure,
 * and the second argument 'r' is the row data of the result set, usually a Map structure.</li>
 * <li>The script engine of the current implementation is 'Oracle Nashorn' that supports java script
 * language. We use CompiledScript to improve performance, which means you have to pay attention to
 * writing scripts, preferably stateless functions, that can be problematic in multi-threaded
 * conditions if global variables are involved.</li>
 * <li>If need another script engine, then implement the ResultMapperResolver interface, and assign
 * an engine name, use this name as the value of the parameter that named 'script-engine' of the
 * hint .</li>
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
  public static final String HNIT_SCRIPT_ENGINE = "script-engine";
  public static final String HINT_SCRIPT_PARA = "p";
  public static final String HINT_SCRIPT_RESU = "r";

  static final ScriptEngineManager sm = new ScriptEngineManager();
  static final Map<QueryHint, ScriptConsumer> mappers = new ConcurrentHashMap<>();
  static final Set<QueryHint> brokens = new CopyOnWriteArraySet<>();

  @Inject
  @Any
  Instance<ResultMapperResolver> mapperResolvers;

  @Override
  public boolean canHandle(Class<?> resultClass, QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), HINT_NAME) && isNotBlank(hint.getScript());
  }

  @Override
  public void handle(QueryHint qh, Object parameter, Object result) throws Exception {
    ScriptConsumer func = null;
    if (brokens.contains(qh) || (func = resolveMapper(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      func.accept(new Object[] {parameter, Map.class.cast(result)});
    } else {
      List<?> list = null;
      if (result instanceof ForwardList) {
        list = ForwardList.class.cast(result).getResults();
      } else if (result instanceof List) {
        list = List.class.cast(result);
      } else if (result instanceof PagedList) {
        list = PagedList.class.cast(result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            func.accept(new Object[] {parameter, Map.class.cast(item)});
          }
        }
      }
    }
  }

  protected ScriptConsumer resolveMapper(QueryHint qh) {
    return mappers.computeIfAbsent(qh, (k) -> {
      if (!mapperResolvers.isUnsatisfied()) {
        Optional<ResultMapperResolver> op =
            mapperResolvers.stream().filter(rmr -> rmr.accept(qh)).findFirst();
        if (op.isPresent()) {
          try {
            return op.get().resolve(qh);
          } catch (Exception e) {
            brokens.add(qh);
            throw new CorantRuntimeException(e);
          }
        }
      }
      brokens.add(qh);
      return (ps) -> {
      };
    });
  }

  @ApplicationScoped
  public static class NashornResultMapperResolver implements ResultMapperResolver {

    public static final String DFLT_SCRIPT_ENGINE = "Oracle Nashorn";

    @Override
    public boolean accept(QueryHint qh) {
      return qh != null && isNotBlank(qh.getScript()) && (isEmpty(qh.getParameters())
          || isEmpty(qh.getParameters(ResultScriptMapperHintHandler.HNIT_SCRIPT_ENGINE))
          || defaultString(
              qh.getParameters(ResultScriptMapperHintHandler.HNIT_SCRIPT_ENGINE).get(0).getValue(),
              DFLT_SCRIPT_ENGINE).equals(DFLT_SCRIPT_ENGINE));
    }

    @Override
    public ScriptConsumer resolve(QueryHint qh) throws Exception {
      return NashornScriptEngines.compileConsumer(qh.getScript(), "p", "r");
    }

  }

  public interface ResultMapperResolver {

    boolean accept(QueryHint qh);

    ScriptConsumer resolve(QueryHint qh) throws Exception;
  }

}

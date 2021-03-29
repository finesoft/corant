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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.mapping.QueryHint.QueryHintParameter;

/**
 * corant-suites-query
 *
 * <p>
 * The result bean mapper hints, use bean to intervene the result.
 * </p>
 * <ul>
 * <li>The key is 'result-bean-mapper'</li>
 * <li>All beans must implement {@link ResultBeanMapper} with a {@link Named} qualifier and beans
 * scope are {@link ApplicationScoped}. The value of the {@link Named} annotation corresponds to the
 * hint parameter 'named', This annotation is used to distinguish bean instances at runtime.</li>
 * </ul>
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
 *       &lt;hint key="result-bean-mapper"&gt;
 *            &lt;parameter name="named" value="theName" /&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 *
 * &#64;ApplicationScope
 * &#64;Named("theName")
 * public class MyResultBeanMapper implement ResultBeanMapper{
 *   public void accept(Object parameter,List<Map<?,?> result){
 *     //TODO
 *   }
 * }
 * </pre>
 * </p>
 *
 * @see ResultScriptMapperHintHandler
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
public class ResultBeanMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-bean-mapper";
  public static final String HNIT_PARA_BEAN_NME = "named";

  protected final Map<QueryHint, Named> nameds = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  @Any
  Instance<ResultBeanMapper> instances;

  @Override
  public boolean canHandle(Class<?> resultClass, QueryHint hint) {
    boolean can = hint != null && areEqual(hint.getKey(), HINT_NAME) && !instances.isUnsatisfied();
    if (can) {
      Named named = resolveBeanNamed(hint);
      can = named != null && instances.select(named).isResolvable();
    }
    return can;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void handle(QueryHint qh, Object parameter, Object result) throws Exception {
    ResultBeanMapper func = resolveBeanMapper(resolveBeanNamed(qh));
    if (func == null) {
      return;
    }
    List<Map<Object, Object>> list = null;
    if (result instanceof Map) {
      list = listOf((Map) result);
    } else {
      if (result instanceof Forwarding) {
        list = ((Forwarding) result).getResults();
      } else if (result instanceof List) {
        list = (List) result;
      } else if (result instanceof Paging) {
        list = ((Paging) result).getResults();
      }
    }
    if (!isEmpty(list)) {
      func.accept(parameter, list);
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    nameds.clear();
    logger.fine(() -> "Clear result bean mapper hint handler caches.");
  }

  protected ResultBeanMapper resolveBeanMapper(Annotation named) {
    if (named == null || instances.isUnsatisfied()) {
      return null;
    } else {
      Instance<ResultBeanMapper> matched = instances.select(named);
      if (matched.isResolvable()) {
        return matched.get();
      }
    }
    return null;
  }

  protected Named resolveBeanNamed(QueryHint qh) {
    return nameds.computeIfAbsent(qh, k -> {
      List<QueryHintParameter> nameds = qh.getParameters(HNIT_PARA_BEAN_NME);
      String named = null;
      if (isNotEmpty(nameds)) {
        named = defaultString(nameds.get(0).getValue(), null);
      }
      return named != null ? NamedLiteral.of(named) : null;
    });
  }

  /**
   * corant-suites-query-shared
   *
   * @author bingo 上午11:33:20
   *
   */
  @FunctionalInterface
  public interface ResultBeanMapper extends BiConsumer<Object, List<Map<Object, Object>>> {

    /**
     * @param queryParmeter the parameters of the query corresponding to hint when executing the
     *        query
     * @param queryResult query result set of query corresponding to hint
     **/
    @Override
    void accept(Object queryParmeter, List<Map<Object, Object>> queryResult);

  }

}

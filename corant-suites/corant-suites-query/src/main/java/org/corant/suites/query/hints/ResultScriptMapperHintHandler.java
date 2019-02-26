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
package org.corant.suites.query.hints;

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
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.Query.ForwardList;
import org.corant.suites.query.Query.PagedList;
import org.corant.suites.query.mapping.QueryHint;
import org.corant.suites.query.spi.ResultHintHandler;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午12:02:08
 *
 */
@SuppressWarnings("restriction")
@ApplicationScoped
public class ResultScriptMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-script-mapper";
  public static final String HNIT_SCRIPT_ENGINE = "script-engine";
  public static final String HINT_SCRIPT_PARA_NME = "dat";

  static final ScriptEngineManager sm = new ScriptEngineManager();
  static final Map<QueryHint, Consumer<Map<?, ?>>> mappers = new ConcurrentHashMap<>();
  static final Set<QueryHint> brokens = new CopyOnWriteArraySet<>();

  @Inject
  @Any
  Instance<ResultMapperResolver> mapperResolvers;

  @Override
  public boolean canHandle(QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), HINT_NAME) && isNotBlank(hint.getScript());
  }

  @Override
  public void handle(QueryHint qh, Object result) throws Exception {
    Consumer<Map<?, ?>> func = null;
    if (brokens.contains(qh) || (func = resolveMapper(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      func.accept(Map.class.cast(result));
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
            func.accept(Map.class.cast(item));
          }
        }
      }
    }
  }

  protected Consumer<Map<?, ?>> resolveMapper(QueryHint qh) {
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
      return (m) -> {
      };
    });
  }

  @ApplicationScoped
  public static class NashornResultMapperResolver implements ResultMapperResolver {

    public static final String DFLT_SCRIPT_ENGINE = "Oracle Nashorn";

    private static final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

    @Override
    public boolean accept(QueryHint qh) {
      return qh != null && isNotBlank(qh.getScript()) && (isEmpty(qh.getParameters())
          || isEmpty(qh.getParameters(ResultScriptMapperHintHandler.HNIT_SCRIPT_ENGINE))
          || defaultString(
              qh.getParameters(ResultScriptMapperHintHandler.HNIT_SCRIPT_ENGINE).get(0).getValue(),
              DFLT_SCRIPT_ENGINE).equals(DFLT_SCRIPT_ENGINE));
    }

    @Override
    public Consumer<Map<?, ?>> resolve(QueryHint qh) throws Exception {
      // -doe Dump a stack trace on errors.
      // --global-per-engine Use single Global instance per script engine instance
      final ScriptEngine scriptEngine =
          factory.getScriptEngine(new String[] {"-doe", "--global-per-engine"});
      final CompiledScript compiled = ((Compilable) scriptEngine).compile(qh.getScript());
      return (m) -> {
        Bindings bindings = new SimpleBindings();
        bindings.put(HINT_SCRIPT_PARA_NME, m);
        try {
          compiled.eval(bindings);
        } catch (ScriptException e) {
          throw new CorantRuntimeException(e);
        }
      };
    }

  }

  public interface ResultMapperResolver {

    boolean accept(QueryHint qh);

    Consumer<Map<?, ?>> resolve(QueryHint qh) throws Exception;
  }

}

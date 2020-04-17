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
package org.corant.suites.query.shared;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.suites.cdi.Instances.resolve;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.corant.Corant;
import org.corant.config.ConfigUtils;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.lang.javascript.NashornScriptEngines;
import org.corant.suites.lang.kotlin.KotlinScriptEngines;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerConfigurations;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryMappingService;
import org.corant.suites.query.shared.mapping.Script.ScriptType;
import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午3:16:46
 *
 */
public class QueryDeveloperKits {

  static final String line = "--------------------------------------------------";

  public static void staticValidate(Object... params) {
    try (Corant corant = prepare()) {
      final QueryMappingService service = resolve(QueryMappingService.class);
      final Map<Object, Object> parameter = mapOf(params);
      out(false);
      List<Throwable> throwabls = new ArrayList<>();
      for (Query q : service.getQueries()) {
        validateQueryScript(q, throwabls, parameter);
        outLine();
      }
      if (isNotEmpty(throwabls)) {
        outLine("Collected Errors");
        for (Throwable t : throwabls) {
          t.printStackTrace();
          outLine();
        }
      }
      out(true);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static void out(boolean end) {
    if (!end) {
      System.out.println("\n/**-->>>>>>>> Schema validate start**/");
    } else {
      System.out.println("\n--<<<<<<<< Schema validate end. **/\n");
    }
  }

  protected static Corant prepare() {
    LoggerFactory.disableLogger();
    ConfigUtils.adjust("webserver.auto-start", "false", "flyway.migrate.enable", "false");
    return Corant.run(QueryDeveloperKits.class, Corant.DISABLE_BOOST_LINE_CMD);
  }

  protected static void validateFetchQueryScript(Query query, FetchQuery fq,
      List<Throwable> throwabls) {
    if (fq.getPredicateScript().isValid() && fq.getPredicateScript().getType() == ScriptType.JS) {
      try {
        NashornScriptEngines.createFunction(fq.getPredicateScript().getCode(), "p", "r");
      } catch (Exception e) {
        throwabls
            .add(new CorantRuntimeException(e, "FETCH-QUERY-PREDICATE-SCRIPT-ERROR : [%s -> %s]",
                query.getName(), fq.getReferenceQuery()));
      }
    }
    if (fq.getInjectionScript().isValid() && fq.getInjectionScript().getType() == ScriptType.JS) {
      try {
        NashornScriptEngines.createFunction(fq.getInjectionScript().getCode(), "r", "fr");
      } catch (Exception e) {
        throwabls.add(new CorantRuntimeException(e, "FETCH-QUERY-INJECT-SCRIPT-ERROR : [%s -> %s]",
            query.getName(), fq.getReferenceQuery()));
      }
    }
    if (fq.getInjectionScript().isValid() && fq.getInjectionScript().getType() == ScriptType.KT) {
      try {
        KotlinScriptEngines.createFunction(fq.getInjectionScript().getCode(), "r", "fr");
      } catch (Exception e) {
        throwabls.add(new CorantRuntimeException(e, "FETCH-QUERY-INJECT-SCRIPT-ERROR : [%s -> %s]",
            query.getName(), fq.getReferenceQuery()));
      }
    }
  }

  protected static void validateQueryScript(Query query, List<Throwable> throwabls,
      Map<Object, Object> parameter) {
    System.out.println("[".concat(query.getName()).concat("]:\n"));
    if (query.getScript().getType() == ScriptType.FM) {
      try (StringWriter sw = new StringWriter()) {
        Template tpl = new Template(query.getName(), query.getScript().getCode(),
            FreemarkerConfigurations.FM_CFG);
        tpl.dump(System.out);
        if (isNotEmpty(parameter)) {
          // DynamicTemplateMethodModelExSql sqlTmm = new DynamicTemplateMethodModelExSql(); FIXME
          // DynamicTemplateMethodModelExJson jsonTmm = new DynamicTemplateMethodModelExJson();
          // FIXME
          System.out.println("\n\n[Empty parameter process]:\n");
          Environment e = tpl.createProcessingEnvironment(parameter, sw);
          // e.setVariable(sqlTmm.getType(), sqlTmm); FIXME
          // e.setVariable(jsonTmm.getType(), jsonTmm); FIXME
          e.process();
          System.out.println(sw.toString());
        }
      } catch (IOException | TemplateException e) {
        throwabls.add(new CorantRuntimeException(e, "QUERY-ERROR : [%s]", query.getName()));
      }
    } else if (query.getScript().getType() == ScriptType.JS) {
      try {
        NashornScriptEngines.createFunction(query.getScript().getCode(), "p", "up");
      } catch (Exception e) {
        throwabls.add(new CorantRuntimeException(e, "QUERY-ERROR : [%s]", query.getName()));
      }
    }
    query.getFetchQueries().forEach(fq -> validateFetchQueryScript(query, fq, throwabls));
  }

  static void outLine(String... strings) {
    System.out
        .println("\n\n".concat(line).concat(String.join(" ", strings)).concat(line).concat("\n\n"));
  }

}

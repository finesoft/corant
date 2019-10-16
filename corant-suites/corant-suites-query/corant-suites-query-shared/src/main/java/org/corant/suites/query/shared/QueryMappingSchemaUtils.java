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

import static org.corant.kernel.util.Instances.resolve;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MapUtils.mapOf;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.corant.Corant;
import org.corant.config.ConfigUtils;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelExJson;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelExSql;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerConfigurations;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryMappingService;
import freemarker.core.Environment;
import freemarker.template.Template;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午3:16:46
 *
 */
public class QueryMappingSchemaUtils {

  static final String line = "--------------------------------------------------";

  public static void staticValidate(Object... params) {
    try (Corant corant = prepare()) {
      final QueryMappingService service = resolve(QueryMappingService.class).get();
      final Map<Object, Object> parameter = mapOf(params);
      out(false);
      for (Query q : service.getQueries()) {
        System.out.println("[".concat(q.getName()).concat("]:\n"));
        try (StringWriter sw = new StringWriter()) {
          Template tpl =
              new Template(q.getName(), q.getScript().getCode(), FreemarkerConfigurations.FM_CFG);
          tpl.dump(System.out);
          if (isNotEmpty(parameter)) {
            DynamicTemplateMethodModelExSql sqlTmm = new DynamicTemplateMethodModelExSql();
            DynamicTemplateMethodModelExJson jsonTmm = new DynamicTemplateMethodModelExJson();
            System.out.println("\n\n[Empty parameter process]:\n");
            Environment e = tpl.createProcessingEnvironment(parameter, sw);
            e.setVariable(sqlTmm.getType(), sqlTmm);
            e.setVariable(jsonTmm.getType(), jsonTmm);
            e.process();
            System.out.println(sw.toString());
          }
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
        System.out.println("\n\n".concat(line).concat(line).concat("\n\n"));
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
    return Corant.run(QueryMappingSchemaUtils.class, Corant.DISABLE_BOOST_LINE_CMD);
  }
}

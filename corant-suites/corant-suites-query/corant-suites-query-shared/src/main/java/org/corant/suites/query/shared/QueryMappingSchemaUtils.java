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
import java.io.IOException;
import org.corant.Corant;
import org.corant.config.ConfigUtils;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerConfigurations;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryMappingService;
import freemarker.template.Template;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午3:16:46
 *
 */
public class QueryMappingSchemaUtils {

  public static void validate() {
    try (Corant corant = prepare()) {
      final QueryMappingService service = resolve(QueryMappingService.class).get();
      String line = "--------------------------------------------------";
      line = "\n" + line + line + "\n\n";
      out(true);
      for (Query q : service.getQueries()) {
        System.out.println(q.getName() + ":\n");
        try {
          new Template(q.getName(), q.getScript(), FreemarkerConfigurations.FM_CFG)
              .dump(System.out);
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
        System.out.println(line);
      }
      out(false);
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

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
package org.corant.modules.query.shared.dynamic.freemarker;

import static java.lang.String.format;
import static org.corant.shared.util.Strings.defaultString;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.inject.Singleton;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.QueryMappingService.AfterQueryMappingInitializedHandler;
import org.corant.modules.query.shared.QueryMappingService.BeforeQueryMappingInitializeHandler;
import org.corant.modules.query.shared.cdi.QueryExtension;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Services;
import org.corant.shared.util.Texts;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-shared
 *
 * <pre>
 * When you try to read a variable, FreeMarker will seek the variable in this order, and stops when
 * it finds a variable with the right name:
 *
 * 1.In the Environment:
 *
 *      1.If you are in a loop, in the set of loop variables. Loop variables are the variables created by
 *      directives like list.
 *
 *      2.If you are inside a macro, in the local variable set of the macro. Local variables can be created
 *      with the local directive. Also, the parameters of macros are local variables.
 *
 *      3.In the current namespace. You can put variables into a namespace with the assign directive.
 *
 *      4.In the set of variables created with global directive. FTL handles these variables as if they
 *      were normal members of the data-model. That is, they are visible in all namespaces, and you can
 *      access them as if they would be in the data-model.
 *
 * 2.In the data-model object you have passed to the process method
 *
 * 3.In the set of shared variables stored in the Configuration
 *
 * </pre>
 *
 * @author bingo 下午5:20:19
 */
@Singleton
public class FreemarkerExecutions
    implements BeforeQueryMappingInitializeHandler, AfterQueryMappingInitializedHandler {

  public static final Configuration FM_CFG = new Configuration(Configuration.VERSION_2_3_33);

  protected static final Logger logger =
      Logger.getLogger(FreemarkerExecutions.class.getCanonicalName());

  protected static final Map<Object, Pair<Template, String>> executions = new ConcurrentHashMap<>();

  protected static final FreemarkerDynamicQueryScriptResolver scriptResolver =
      Services.findRequired(FreemarkerDynamicQueryScriptResolver.class)
          .orElse(FreemarkerDynamicQueryScriptResolver.DEFAULT_INST);

  static {
    FM_CFG.setNumberFormat("computer");
    /* See https://freemarker.apache.org/docs/dgui_misc_computer_vs_human_format.html */
    // FM_CFG.setCFormat(JSONCFormat.INSTANCE); TODO FIXME use c_format explicitly
    FM_CFG.setDefaultEncoding("UTF-8");
    FM_CFG.setLogTemplateExceptions(false);
    FM_CFG.setFallbackOnNullLoopVariable(false);
    FM_CFG.setWrapUncheckedExceptions(true);
  }

  protected FreemarkerExecutions() {}

  public static Pair<Template, String> resolveExecution(Query query) {
    return executions.computeIfAbsent(query.getScript().getId(), k -> {
      String script = null;
      try {
        script = defaultString(scriptResolver.resolve(query));
        return Pair.of(new Template(query.getVersionedName(), script, FM_CFG), script);
      } catch (ParseException pe) {
        throw new QueryRuntimeException(pe,
            "An error occurred while executing the query template [%s]. %s",
            query.getVersionedName(), resolveScriptExceptionInfo(script, pe));
      } catch (Exception e) {
        throw new QueryRuntimeException(e,
            "An error occurred while executing the query template [%s].", query.getVersionedName());
      }
    });
  }

  public static String resolveScriptExceptionInfo(String script, ParseException pe) {
    return format("Source script:%n>>>%n%s%n<<<", Texts.labelLineAndColumn(script,
        pe.getLineNumber(), pe.getColumnNumber(), pe.getEndLineNumber(), pe.getEndColumnNumber()));
  }

  public static String resolveScriptExceptionInfo(String script, TemplateException pe) {
    return format("Source script:%n>>>%n%s%n<<<", Texts.labelLineAndColumn(script,
        pe.getLineNumber(), pe.getColumnNumber(), pe.getEndLineNumber(), pe.getEndColumnNumber()));
  }

  public static FreemarkerDynamicQueryScriptResolver scriptResolver() {
    return scriptResolver;
  }

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void afterQueryMappingInitialized(Collection<Query> queries, long initializedVersion) {
    executions.clear();
    if (QueryExtension.verifyDeployment) {
      logger.info("Start freemarker query scripts pre-compiling.");
      int cs = 0;
      for (Query query : queries) {
        if (query.getScript().getType() == ScriptType.FM) {
          resolveExecution(query);
          cs++;
        }
      }
      logger.info("Complete " + cs + " freemarker query scripts pre-compiling.");
    }
  }

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void beforeQueryMappingInitialize(Collection<Query> queries, long initializedVersion) {
    executions.clear();
  }
}

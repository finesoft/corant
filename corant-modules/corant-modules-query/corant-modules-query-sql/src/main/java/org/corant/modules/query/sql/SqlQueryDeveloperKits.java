/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.sql;

import static java.util.Collections.emptyMap;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.corant.modules.datasource.shared.DBMS;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.QueryMappingService;
import org.corant.modules.query.shared.dynamic.freemarker.FreemarkerExecutions;
import org.corant.modules.query.sql.cdi.SqlNamedQueryServiceManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Immutable.ImmutableSetBuilder;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.ValidationError;
import net.sf.jsqlparser.util.validation.ValidationException;
import net.sf.jsqlparser.util.validation.feature.DatabaseType;
import net.sf.jsqlparser.util.validation.metadata.JdbcDatabaseMetaDataCapability;
import net.sf.jsqlparser.util.validation.metadata.NamesLookup;

/**
 * corant-modules-query-sql
 *
 * @author bingo 14:15:32
 */
public class SqlQueryDeveloperKits {

  public static final Set<String> DEFAULT_DIRECTIVE_PATTERNS =
      new ImmutableSetBuilder<>("<[#@].*?>\\n", "<[#@].*?>\\r", "<[#@].*?>", "</[#@].*?>\\n",
          "</[#@].*?>\\r", "</[#@].*?>").build();

  public static final Set<String> DEFAULT_EX_MACRO_DIRECTIVE_PATTERNS =
      new ImmutableSetBuilder<>("<#(?!macro).*?>\\n", "<#(?!macro).*?>\\r", "<#(?!macro).*?>",
          "</#(?!macro).*?>\\n", "</#(?!macro).*?>\\r", "</#(?!macro).*?>").build();

  public static final Set<String> DEFAULT_VARIABLE_PATTERNS =
      new ImmutableSetBuilder<>("\\$\\{.*?}").build();

  public static final Set<String> DEFAULT_TM_VARIABLE_PATTERNS =
      new ImmutableSetBuilder<>("\\$\\{TM.*?}").build();

  @Experimental
  public static FreemarkerQueryScriptValidator freemarkerQueryScriptValidator() {
    return new FreemarkerQueryScriptValidator();
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 18:48:09
   */
  public static class FreemarkerQueryScriptValidator {
    Map<String, String> specVarReplacements = new LinkedHashMap<>();
    Map<String, String> specVarPatternReplacements = new LinkedHashMap<>();
    Set<String> directivePatterns = DEFAULT_DIRECTIVE_PATTERNS;
    Set<String> variablePatterns = DEFAULT_VARIABLE_PATTERNS;
    String defaultVariableReplacement = "NULL";
    String defaultDirectiveReplacement = "";
    FeatureConfiguration featureConfiguration = new FeatureConfiguration();
    boolean includeMacro = false;
    Set<String> skipQueryQualifier = new HashSet<>();
    Predicate<String> queryNameSkiper = Functions.emptyPredicate(false);

    public FreemarkerQueryScriptValidator defaultDirectiveReplacement(
        String defaultDirectiveReplacement) {
      this.defaultDirectiveReplacement = defaultDirectiveReplacement;
      return this;
    }

    public FreemarkerQueryScriptValidator defaultVariableReplacement(
        String defaultVariableReplacement) {
      this.defaultVariableReplacement = defaultVariableReplacement;
      return this;
    }

    public FreemarkerQueryScriptValidator featureConfiguration(
        FeatureConfiguration featureConfiguration) {
      if (featureConfiguration != null) {
        this.featureConfiguration = featureConfiguration;
      }
      return this;
    }

    public FreemarkerQueryScriptValidator includeMacro(boolean includeMacro) {
      this.includeMacro = includeMacro;
      if (this.includeMacro) {
        directivePatterns = DEFAULT_EX_MACRO_DIRECTIVE_PATTERNS;
        variablePatterns = DEFAULT_TM_VARIABLE_PATTERNS;
      } else {
        directivePatterns = DEFAULT_DIRECTIVE_PATTERNS;
        variablePatterns = DEFAULT_VARIABLE_PATTERNS;
      }
      return this;
    }

    public FreemarkerQueryScriptValidator putSpecVarPatternReplacement(String string,
        String replacement) {
      specVarPatternReplacements.put(string, replacement);
      return this;
    }

    public FreemarkerQueryScriptValidator putSpecVarReplacement(String string, String replacement) {
      specVarReplacements.put(string, replacement);
      return this;
    }

    public FreemarkerQueryScriptValidator queryNameSkiper(Predicate<String> queryNameSkiper) {
      if (this.queryNameSkiper != null) {
        this.queryNameSkiper = queryNameSkiper;
      } else {
        this.queryNameSkiper = Functions.emptyPredicate(false);
      }
      return this;
    }

    public FreemarkerQueryScriptValidator removeSpecVarPatternReplacementIf(
        Predicate<String> predicate) {
      if (predicate != null) {
        List<String> removeKeys =
            specVarPatternReplacements.keySet().stream().filter(predicate).toList();
        if (isNotEmpty(removeKeys)) {
          removeKeys.forEach(specVarPatternReplacements::remove);
        }
      }
      return this;
    }

    public FreemarkerQueryScriptValidator removeSpecVarReplacementIf(Predicate<String> predicate) {
      if (predicate != null) {
        List<String> removeKeys = specVarReplacements.keySet().stream().filter(predicate).toList();
        if (isNotEmpty(removeKeys)) {
          removeKeys.forEach(specVarReplacements::remove);
        }
      }
      return this;
    }

    public void validate() {
      try {
        final QueryMappingService service = resolve(QueryMappingService.class);
        final SqlNamedQueryServiceManager sqlQueryService =
            resolve(SqlNamedQueryServiceManager.class);
        final DataSourceService dataSources = resolve(DataSourceService.class);
        Map<String, List<ValidationError>> errors = new LinkedHashMap<>();
        boolean hasErrors = false;
        for (Query query : service.getQueries()) {
          if (query.getScript().getType() != ScriptType.FM || query.getType() != QueryType.SQL
              || skipQueryQualifier.contains(query.getQualifier())
              || queryNameSkiper.test(query.getVersionedName())) {
            System.out.println("[SKIP]: " + query.getVersionedName());
            continue;
          }
          String script = null;
          try {
            script = getScript(query);
          } catch (Exception e) {
            errors.put(query.getVersionedName(),
                listOf(new ValidationError("Parse script occurred error!")
                    .addError(new ValidationException(e))));
          }
          if (isBlank(script)) {
            System.out.println("[INVALID]: " + query.getVersionedName() + " script extract error!");
            continue;
          }
          Pair<DBMS, String> dss = sqlQueryService.resolveDataSourceSchema(query.getQualifier());
          String dsName = dss.right();
          DatabaseType dsType = DatabaseType.ANSI_SQL;
          switch (dss.left()) {
            case ORACLE:
              dsType = DatabaseType.ORACLE;
              break;
            case POSTGRE:
              dsType = DatabaseType.POSTGRESQL;
              break;
            case H2:
              dsType = DatabaseType.H2;
              break;
            case MYSQL:
              dsType = DatabaseType.MYSQL;
              break;
            case SQLSERVER2005:
            case SQLSERVER2008:
            case SQLSERVER2012:
              dsType = DatabaseType.SQLSERVER;
              break;
            default:
              break;
          }
          try (Connection conn = dataSources.resolve(dsName).getConnection()) {
            Validation validation = new Validation(featureConfiguration,
                Arrays.asList(dsType,
                    new JdbcDatabaseMetaDataCapability(conn, NamesLookup.NO_TRANSFORMATION)),
                script);
            List<ValidationError> validationErrors = validation.validate();
            errors.put(query.getVersionedName(), validationErrors);
            if (isEmpty(validationErrors)) {
              System.out.println("[VALID]: " + query.getVersionedName());
            } else {
              hasErrors = true;
              System.out.println("[INVALID]: " + query.getVersionedName() + " ["
                  + validationErrors.size() + "] ERRORS");
            }
          }

        }
        if (hasErrors) {
          System.out.println("[VALIDATION]: completed, output error messages");
          System.out.println("*".repeat(100));
          errors.forEach((k, v) -> {
            if (isNotEmpty(v)) {
              v.forEach(e -> {
                if (isNotEmpty(e.getErrors())) {
                  System.out.println("[QUERY NAME]: " + k);
                  System.out.println("[ERROR SQL]:\n" + e.getStatements());
                  System.out.println("[ERROR MESSAGE]:");
                  e.getErrors().stream().map(
                      (Function<? super ValidationException, ? extends String>) ValidationException::getMessage)
                      .forEach(System.out::println);
                  System.out.println("*".repeat(100));
                }
              });
            }
          });
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    protected String getScript(Query query) throws TemplateException, IOException {
      String script = query.getScript().getCode();
      String macro = query.getMacroScript();
      String text = script;
      if (includeMacro && isNotBlank(macro)) {
        text = macro + "\n" + script;
        try (StringWriter sw = new StringWriter()) {
          text = cleanFreemarkerTargets(text);
          new Template(query.getVersionedName(), text, FreemarkerExecutions.FM_CFG)
              .createProcessingEnvironment(emptyMap(), sw).process();
          text = sw.toString();
          MutableString result = new MutableString(text);
          for (String vp : DEFAULT_VARIABLE_PATTERNS) {
            result.apply(r -> r.replaceAll(vp, defaultVariableReplacement));
          }
          for (String dp : DEFAULT_DIRECTIVE_PATTERNS) {
            result.apply(r -> r.replaceAll(dp, defaultDirectiveReplacement));
          }
          text = result.get();
        }
      } else {
        text = cleanFreemarkerTargets(text);
      }
      return text;
    }

    String cleanFreemarkerTargets(String text) {
      if (isNotBlank(text)) {
        MutableString result = new MutableString(text);
        if (specVarReplacements != null) {
          specVarReplacements.forEach((k, v) -> result.apply(r -> Strings.replace(r, k, v)));
        }
        if (specVarPatternReplacements != null) {
          specVarPatternReplacements.forEach((k, v) -> result.apply(r -> r.replaceAll(k, v)));
        }
        for (String vp : variablePatterns) {
          result.apply(r -> r.replaceAll(vp, defaultVariableReplacement));
        }
        for (String dp : directivePatterns) {
          result.apply(r -> r.replaceAll(dp, defaultDirectiveReplacement));
        }
        return result.get();
      }
      return text;
    }
  }
}

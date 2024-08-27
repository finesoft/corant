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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.sql.DataSource;
import org.corant.modules.datasource.shared.DBMS;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.QueryMappingService;
import org.corant.modules.query.sql.cdi.SqlNamedQueryServiceManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Immutable.ImmutableSetBuilder;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Strings;
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

  public static final Set<String> DEFAULT_VARIABLE_PATTERNS =
      new ImmutableSetBuilder<>("\\$\\{.*?}").build();

  @Experimental
  public static String cleanFreemarkerTargets(String text, String variableReplacement,
      String directiveReplacement, Map<String, String> specificVariableReplacements) {
    if (isNotBlank(text)) {
      MutableString result = new MutableString(text);
      if (specificVariableReplacements != null) {
        specificVariableReplacements.forEach((k, v) -> result.apply(r -> Strings.replace(r, k, v)));
      }
      for (String vp : DEFAULT_VARIABLE_PATTERNS) {
        result.apply(r -> r.replaceAll(vp, variableReplacement));
      }
      for (String dp : DEFAULT_DIRECTIVE_PATTERNS) {
        result.apply(r -> r.replaceAll(dp, directiveReplacement));
      }
      return result.get();
    }
    return text;
  }

  @Experimental
  public static void validateFreemarkerSQLQueryScriptStatically(String variableReplacement,
      String directiveReplacement) {
    validateFreemarkerSQLQueryScriptStatically(variableReplacement, directiveReplacement,
        emptyMap());
  }

  @Experimental
  public static void validateFreemarkerSQLQueryScriptStatically(String variableReplacement,
      String directiveReplacement, Map<String, String> specificVariableReplacements) {
    validateFreemarkerSQLQueryScriptStatically(variableReplacement, directiveReplacement,
        specificVariableReplacements, null);
  }

  @Experimental
  public static void validateFreemarkerSQLQueryScriptStatically(String variableReplacement,
      String directiveReplacement, Map<String, String> specificVariableReplacements,
      FeatureConfiguration validatorFeatureConfig) {
    try {
      final QueryMappingService service = resolve(QueryMappingService.class);
      final SqlNamedQueryServiceManager sqlQueryService =
          resolve(SqlNamedQueryServiceManager.class);
      final DataSourceService dataSources = resolve(DataSourceService.class);
      Map<String, List<ValidationError>> errors = new LinkedHashMap<>();
      for (Query query : service.getQueries()) {
        if (query.getScript().getType() != ScriptType.FM || query.getType() != QueryType.SQL) {
          continue;
        }
        String script = cleanFreemarkerTargets(query.getScript().getCode(), variableReplacement,
            directiveReplacement, specificVariableReplacements);
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
        DataSource ds = dataSources.resolve(dsName);
        Validation validation =
            new Validation(defaultObject(validatorFeatureConfig, FeatureConfiguration::new),
                Arrays.asList(dsType,
                    new JdbcDatabaseMetaDataCapability(ds.getConnection(), NamesLookup.LOWERCASE)),
                script);
        errors.put(query.getVersionedName(), validation.validate());
      }
      errors.forEach((k, v) -> {
        if (isNotEmpty(v)) {
          v.forEach(e -> {
            if (isNotEmpty(e.getErrors())) {
              System.out.println("[QUERY NAME]:\n" + k);
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
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }
}

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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyConsumer;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.transform;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.trim;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.modules.datasource.shared.DBMS;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.json.ObjectMappers;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.Script.ScriptType;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.modules.query.shared.QueryMappingService;
import org.corant.modules.query.shared.dynamic.freemarker.FreemarkerExecutions;
import org.corant.modules.query.shared.dynamic.jsonexpression.JsonExpressionScriptProcessor;
import org.corant.modules.query.shared.spi.ResultMapReduceHintHandler;
import org.corant.modules.query.sql.cdi.SqlNamedQueryServiceManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Immutable.ImmutableSetBuilder;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import com.fasterxml.jackson.core.JsonProcessingException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
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
      new ImmutableSetBuilder<>("\\$\\{TM.*?}", "\\$\\{CM.*?}").build();

  public static final String JSE_RESULT_PARAM_PATTERN = "@r\\.[.a-zA-Z0-9]*";

  public static final String JSE_FETCH_RESULT_PARAM_PATTERN = "@fr\\.[.a-zA-Z0-9]*";

  @Experimental
  public static FreemarkerQueryScriptValidator freemarkerQueryScriptValidator() {
    return new FreemarkerQueryScriptValidator();
  }

  @Experimental
  public static List<String> resolveJSEResultVariableNames(String express, boolean fetch) {
    if (isNotBlank(express)) {
      Pattern pattern =
          Pattern.compile(fetch ? JSE_FETCH_RESULT_PARAM_PATTERN : JSE_RESULT_PARAM_PATTERN);
      Matcher matcher = pattern.matcher(express);
      List<String> matches = new ArrayList<>();
      while (matcher.find()) {
        String name = matcher.group();
        if (fetch && name.length() > 4) {
          name = name.substring(4);
        } else if (name.length() > 3) {
          name = name.substring(3);
        }
        matches.add(name);
      }
      return unmodifiableList(matches);
    }
    return emptyList();
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 18:48:09
   */
  public static class FreemarkerQueryScriptValidator {
    protected Map<String, String> specVarReplacements = new LinkedHashMap<>();
    protected Map<String, String> specVarPatternReplacements = new LinkedHashMap<>();
    protected Set<String> directivePatterns = DEFAULT_DIRECTIVE_PATTERNS;
    protected Set<String> variablePatterns = DEFAULT_VARIABLE_PATTERNS;
    protected String defaultVariableReplacement = "NULL";
    protected String defaultDirectiveReplacement = "";
    protected FeatureConfiguration featureConfiguration = new FeatureConfiguration();
    protected boolean includeMacro;
    protected boolean includeFetchQueryHandling;
    protected boolean includeMapReduceHints;
    protected boolean outputReplacedDirectiveStacks;
    protected boolean outputProcessedValidScript;
    protected Set<String> skipQueryQualifier = new HashSet<>();
    protected Predicate<String> queryNameSkipper = Functions.emptyPredicate(false);
    protected Predicate<String> queryNameFilter = Functions.emptyPredicate(true);
    protected Map<String, Object> directiveStacksReplacements = new LinkedHashMap<>();
    protected Consumer<String> validatingInfoHandler = System.out::println;
    protected BiConsumer<String, List<ValidationError>> validationErrorHandler =
        (k, v) -> v.forEach(e -> {
          if (isNotEmpty(e.getErrors())) {
            System.err.println("[QUERY NAME]: " + k);
            if (isNotBlank(e.getStatements())) {
              System.err.println("[ERROR SQL]:\n" + e.getStatements());
            }
            System.err.println("[ERROR MESSAGE]:");
            e.getErrors().stream().map(
                (Function<? super ValidationException, ? extends String>) ValidationException::getMessage)
                .forEach(System.err::println);
            System.err.println("*".repeat(100));
          }
        });

    public FreemarkerQueryScriptValidator addSkipQueryQualifier(String... skipQueryQualifiers) {
      Collections.addAll(skipQueryQualifier, skipQueryQualifiers);
      return this;
    }

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

    @Experimental
    public FreemarkerQueryScriptValidator includeFetchQueryHandling(
        boolean includeFetchQueryHandling) {
      this.includeFetchQueryHandling = includeFetchQueryHandling;
      return this;
    }

    @Experimental
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

    public FreemarkerQueryScriptValidator includeMapReduceHints(boolean includeMapReduceHints) {
      this.includeMapReduceHints = includeMapReduceHints;
      return this;
    }

    public FreemarkerQueryScriptValidator outputProcessedValidScript(
        boolean outputProcessedValidScript) {
      this.outputProcessedValidScript = outputProcessedValidScript;
      return this;
    }

    public FreemarkerQueryScriptValidator outputReplacedDirectiveStacks(
        boolean outputReplacedDirectiveStacks) {
      this.outputReplacedDirectiveStacks = outputReplacedDirectiveStacks;
      return this;
    }

    public FreemarkerQueryScriptValidator putDirectiveStackReplacement(String directiveStack,
        String replacement) {
      if (isNotBlank(directiveStack)) {
        directiveStacksReplacements.put(trim(directiveStack), trim(replacement));
      }
      return this;
    }

    public FreemarkerQueryScriptValidator putDirectiveStackReplacements(String directiveStack,
        Map<String, String> specQueryNameReplacements) {
      if (isNotBlank(directiveStack)) {
        if (isEmpty(specQueryNameReplacements)) {
          directiveStacksReplacements.put(trim(directiveStack), null);
        } else {
          directiveStacksReplacements.put(trim(directiveStack),
              transform(specQueryNameReplacements,
                  (Function<? super String, ? extends String>) Strings::defaultString,
                  (Function<? super String, ? extends String>) Strings::defaultTrim));
        }
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

    public FreemarkerQueryScriptValidator queryNameFilter(Predicate<String> queryNameFilter) {
      if (this.queryNameFilter != null) {
        this.queryNameFilter = queryNameFilter;
      } else {
        this.queryNameFilter = Functions.emptyPredicate(true);
      }
      return this;
    }

    public FreemarkerQueryScriptValidator queryNameSkipper(Predicate<String> queryNameSkipper) {
      if (this.queryNameSkipper != null) {
        this.queryNameSkipper = queryNameSkipper;
      } else {
        this.queryNameSkipper = Functions.emptyPredicate(false);
      }
      return this;
    }

    public FreemarkerQueryScriptValidator removeDirectiveStackReplacementsIf(
        Predicate<String> predicate) {
      if (predicate != null) {
        List<String> removeKeys =
            directiveStacksReplacements.keySet().stream().filter(predicate).toList();
        if (isNotEmpty(removeKeys)) {
          removeKeys.forEach(directiveStacksReplacements::remove);
        }
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
        final ResultMapReduceHintHandler mapReduceHintHandler =
            resolve(ResultMapReduceHintHandler.class);
        Map<String, List<ValidationError>> errorMaps = new LinkedHashMap<>();
        Map<String, List<String>> queryFieldNames = new LinkedHashMap<>();
        Set<String> validatedQueries = new HashSet<>();
        for (Query query : service.getQueries()) {
          if (!validatedQueries.contains(query.getVersionedName())) {
            validateQuery(service, sqlQueryService, dataSources, mapReduceHintHandler, errorMaps,
                queryFieldNames, query, validatedQueries);
          }
        }
        boolean hasErrors = errorMaps.values().stream().anyMatch(es -> !es.isEmpty());
        validatingInfoHandler
            .accept("Validation completed" + (hasErrors ? ", some errors were found" : EMPTY));
        if (hasErrors) {
          errorMaps.forEach((k, v) -> {
            if (isNotEmpty(v)) {
              validationErrorHandler.accept(k, v);
            }
          });
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    public void validateQuery(final QueryMappingService service,
        final SqlNamedQueryServiceManager sqlQueryService, final DataSourceService dataSources,
        final ResultMapReduceHintHandler mapReduceHintHandler,
        Map<String, List<ValidationError>> errorMaps, Map<String, List<String>> queryFieldNames,
        Query query, Set<String> validatedQueries) {

      final String queryName = query.getVersionedName();
      if (query.getScript().getType() != ScriptType.FM || resolveQueryType(query) != QueryType.SQL
          || skipQueryQualifier.contains(query.getQualifier()) || queryNameSkipper.test(queryName)
          || !queryNameFilter.test(queryName)) {
        validatingInfoHandler.accept("[SKIP]: " + queryName);
        validatedQueries.add(queryName);
        return;
      }

      List<ValidationError> errors = errorMaps.computeIfAbsent(queryName, k1 -> new ArrayList<>());

      String script = null;
      try {
        script = getScript(query);
      } catch (Exception e) {
        errors.add(createValidationError("Parse script occurred error!", e));
      }
      if (isBlank(script)) {
        errors.add(createValidationError("[INVALID]: " + queryName + " script extract error!"));
        validatedQueries.add(queryName);
        return;
      }
      Pair<DBMS, String> dss =
          sqlQueryService.resolveDataSourceSchema(resolveQueryQualifier(query));
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
        Validation validation =
            new Validation(featureConfiguration,
                Arrays.asList(dsType,
                    new JdbcDatabaseMetaDataCapability(conn, NamesLookup.NO_TRANSFORMATION)),
                script);

        // validate query self
        errors.addAll(validation.validate());

        List<String> fieldNames = resolveFiledNames(validation);

        // validate fetch queries if necessary
        if (isNotEmpty(query.getFetchQueries()) && includeFetchQueryHandling) {
          for (FetchQuery fetchQuery : query.getFetchQueries()) {
            final String fetchQueryName = fetchQuery.getQueryReference().getVersionedName();
            Query usedFetchQuery = service.getQuery(fetchQueryName);
            if (canValidateFetchQuery(query, fetchQuery, usedFetchQuery)) {
              if (!validatedQueries.contains(fetchQueryName)) {
                validateQuery(service, sqlQueryService, dataSources, mapReduceHintHandler,
                    errorMaps, queryFieldNames, usedFetchQuery, validatedQueries);
              }
              List<String> fetchQueryFieldNames =
                  queryFieldNames.getOrDefault(fetchQueryName, new ArrayList<>());
              validateFetchQuery(fieldNames, errors, fetchQuery, fetchQueryFieldNames);
            } else {
              validatingInfoHandler.accept(
                  "[SKIP]: fetch query " + fetchQuery.getQueryReference().getVersionedName());
            }
          }
        }

        // validate map reduce query hints if necessary
        if (isNotEmpty(query.getHints()) && includeMapReduceHints) {
          final List<String> cfns = fieldNames;
          Set<String> appendFieldNames = new LinkedHashSet<>();
          for (QueryHint qh : query.getHints()) {
            if (mapReduceHintHandler.supports(Map.class, qh) && isNotEmpty(cfns)) {
              String mapName = mapReduceHintHandler.resolveMapFieldname(qh);
              if (isNotBlank(mapName)) {
                appendFieldNames.add(mapName);
              }
              mapReduceHintHandler.resolveReduceFields(qh).stream().map(t -> t.getMiddle()[0])
                  .forEach(rf -> {
                    if (!cfns.contains(rf)) {
                      errors.add(createValidationError(
                          "Map reduce query hint reduce field [" + rf + "] not exits!"));
                    }
                  });
            }
          }
          if (!appendFieldNames.isEmpty()) {
            cfns.addAll(appendFieldNames);
          }
        }

        queryFieldNames.put(queryName, fieldNames);

        if (isEmpty(errors)) {
          validatingInfoHandler.accept("[VALID]: " + queryName);
          if (outputProcessedValidScript) {
            validatingInfoHandler.accept("[SCRIPT]");
            validatingInfoHandler.accept(script);
          }
        } else {
          validatingInfoHandler
              .accept("[INVALID]: " + queryName + " [" + errors.size() + "] ERRORS");
        }
      } catch (Exception ex) {
        errors.add(createValidationError("Validation occurred error!", ex));
      } finally {
        validatedQueries.add(queryName);
      }
    }

    public FreemarkerQueryScriptValidator validatingInfoHandler(
        Consumer<String> validatingInfoHandler) {
      if (validatingInfoHandler != null) {
        this.validatingInfoHandler = validatingInfoHandler;
      } else {
        this.validatingInfoHandler = emptyConsumer();
      }
      return this;
    }

    public FreemarkerQueryScriptValidator validationErrorHandler(
        BiConsumer<String, List<ValidationError>> validationErrorHandler) {
      if (validationErrorHandler != null) {
        this.validationErrorHandler = validationErrorHandler;
      }
      return this;
    }

    protected boolean canValidateFetchQuery(Query parentQuery, FetchQuery fetchQuery,
        Query usedQuery) {
      if (fetchQuery.getInlineQueryName() == null
          && fetchQuery.getReferenceQueryQualifier() != null) {
        // specified reference query qualifier not support
        return false;
      }
      return resolveFetchQueryType(parentQuery, fetchQuery, usedQuery) == QueryType.SQL;
    }

    protected String cleanFreemarkerTargets(String text) {
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

    protected ValidationError createValidationError(String msg) {
      return createValidationError(msg, null);
    }

    protected ValidationError createValidationError(String msg, Throwable t) {
      ValidationError ve = new ValidationError(EMPTY);
      if (t instanceof ValidationException x) {
        ve.addError(x);
      } else if (t != null) {
        ve.addError(new ValidationException(t));
      } else {
        ve.addError(new ValidationException(msg));
      }
      return ve;
    }

    protected String getScript(Query query) throws TemplateException, IOException {
      String script = query.getScript().getCode();
      String macro = query.getMacroScript();
      String text = script;
      if (includeMacro && isNotBlank(macro)) {
        text = replaceDirectiveStacksNecessary(query, macro + Systems.getLineSeparator() + script);
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
        text = cleanFreemarkerTargets(replaceDirectiveStacksNecessary(query, text));
      }
      return text;
    }

    protected String replaceDirectiveStacksNecessary(Query query, String string) {
      if (isEmpty(directiveStacksReplacements)) {
        return string;
      }

      List<String> lines = string.lines().toList();

      List<Pair<List<Integer>, Object>> collectedRepLns = new ArrayList<>();
      directiveStacksReplacements.forEach((k, v) -> {
        List<List<Integer>> tmp = resolveReplaceDirectiveStacksLines(k, lines);
        if (isNotEmpty(tmp)) {
          for (List<Integer> tmpLs : tmp) {
            if (isNotEmpty(tmpLs)) {
              collectedRepLns.add(Pair.of(new ArrayList<>(tmpLs), v));
            }
          }
        }
      });

      if (isEmpty(collectedRepLns)) {
        return string;
      }

      // sort the replacement line numbers
      collectedRepLns.sort(Comparator.comparing(p -> p.left().get(0)));

      // remove the intersect line numbers
      List<Pair<List<Integer>, Object>> repLns = new ArrayList<>();
      List<Integer> pre = null;
      for (Pair<List<Integer>, Object> tmp : collectedRepLns) {
        if (pre == null || pre.stream().noneMatch(tmp.left()::contains)) {
          repLns.add(tmp);
          pre = tmp.left();
        }
      }
      if (repLns.isEmpty()) {
        return string;
      }

      StringBuilder sb = new StringBuilder();
      String lineSpr = Systems.getLineSeparator();
      StringBuilder re = new StringBuilder();
      int size = lines.size();
      Pair<List<Integer>, Object> matches = null;
      for (int i = 0; i < size; i++) {
        if (matches == null) {
          for (Pair<List<Integer>, Object> repLn : repLns) {
            if (repLn.left().contains(i)) {
              matches = repLn;
              repLns.remove(repLn);
              break;
            }
          }
        }
        String line = lines.get(i);
        if (matches == null) {
          sb.append(line).append(lineSpr);
        } else {
          matches.left().remove(Integer.valueOf(i));
          if (matches.left().isEmpty()) {
            if (matches.right() != null) {
              Object replacement = matches.right();
              if (replacement instanceof String) {
                sb.append(replacement).append(lineSpr);
              } else if (replacement instanceof Map<?, ?> mr) {
                String x = getMapString(mr, query.getVersionedName());
                if (x != null) {
                  sb.append(x).append(lineSpr);
                }
              }
            }
            matches = null;
          }
          if (outputReplacedDirectiveStacks) {
            re.append("|- ").append(line).append(lineSpr);
          }
        }
      }
      if (outputReplacedDirectiveStacks && !re.isEmpty()) {
        validatingInfoHandler.accept("[STACK-REP]:");
        validatingInfoHandler.accept("-".repeat(100));
        validatingInfoHandler.accept(re.substring(0, re.length() - lineSpr.length()));
        validatingInfoHandler.accept("-".repeat(100));
      }
      return sb.toString();
    }

    protected QueryType resolveFetchQueryType(Query parentQuery, FetchQuery fetchQuery,
        Query actualFetchQuery) {
      if (fetchQuery.getInlineQueryName() != null) {
        return defaultObject(fetchQuery.getInlineQueryType(),
            NamedQueryServiceManager.DEFAULT_QUERY_TYPE);
      }
      if (fetchQuery.getReferenceQueryType() != null) {
        return fetchQuery.getReferenceQueryType();
      }
      if (actualFetchQuery.getType() != null) {
        return actualFetchQuery.getType();
      }
      return resolveQueryType(parentQuery);
    }

    protected List<String> resolveFiledNames(Validation validation) {
      if (validation.getParsedStatements() != null
          && isNotEmpty(validation.getParsedStatements().getStatements())) {
        for (Statement st : validation.getParsedStatements().getStatements()) {
          if ((st instanceof Select select) && (select.getSelectBody() instanceof PlainSelect ps)) {
            List<String> fieldNames = new ArrayList<>();
            for (SelectItem item : ps.getSelectItems()) {
              if (item instanceof SelectExpressionItem si) {
                if (si.getAlias() != null) {
                  fieldNames.add(trim(si.getAlias().getName()));
                } else {
                  // FIXME use . ??
                  String fn = si.getExpression().toString();
                  int dot = fn.indexOf('.');
                  if (dot != -1 && fn.length() > (dot + 1)) {
                    fieldNames.add(trim(fn.substring(dot + 1)));
                  } else {
                    fieldNames.add(trim(fn));
                  }
                }
              }
            }
            // FIXME AllColumns AllTableColumns
            return fieldNames;
          }
        }
      }
      return new ArrayList<>();
    }

    protected String resolveQueryQualifier(Query query) {
      if (query == null) {
        return null;
      }
      return defaultObject(query.getQualifier(), NamedQueryServiceManager.DEFAULT_QUALIFIER);
    }

    protected QueryType resolveQueryType(Query query) {
      if (query == null) {
        return null;
      }
      return defaultObject(query.getType(), NamedQueryServiceManager.DEFAULT_QUERY_TYPE);
    }

    protected List<List<Integer>> resolveReplaceDirectiveStacksLines(String sd,
        List<String> lines) {
      String usd = trim(sd);
      int s = usd.indexOf(' ');
      String start = usd.substring(0, s);
      String end = "</" + usd.substring(1, s) + ">";
      List<List<Integer>> poses = new ArrayList<>();
      List<Integer> stackPoses = new ArrayList<>();
      int lineNo = 0;
      Stack<String> stack = new Stack<>();
      boolean inStack = false;
      for (String line : lines) {
        String tl = trim(line);
        if (inStack) {
          stackPoses.add(lineNo);
          if (tl.startsWith(start)) {
            stack.push(tl);
          } else if (tl.equals(end)) {
            stack.pop();
            if (stack.isEmpty()) {
              poses.add(new ArrayList<>(stackPoses));
              inStack = false;
              stackPoses.clear();
            }
          }
        } else if (tl.equals(usd)) {
          stackPoses.add(lineNo);
          stack.push(tl);
          inStack = true;
          if (tl.endsWith("/>")) {
            stack.clear();
            poses.add(new ArrayList<>(stackPoses));
            inStack = false;
            stackPoses.clear();
          }
        }
        lineNo++;
      }
      return poses;
    }

    protected void validateFetchQuery(List<String> fieldNames, List<ValidationError> errors,
        FetchQuery fq, List<String> fetchQueryFieldNames) {

      Set<String> injectFieldNames = new LinkedHashSet<>();

      // check JSE predicate script variables
      String fqn = fq.getQueryReference().getVersionedName();
      if (isNotEmpty(fieldNames) && fq.getPredicateScript().isValid()
          && fq.getPredicateScript().getType() == ScriptType.JSE) {
        List<String> resultParamNames =
            resolveJSEResultVariableNames(fq.getPredicateScript().getCode(), false);
        if (isNotEmpty(resultParamNames)) {
          for (String rpn : resultParamNames) {
            if (!fieldNames.contains(trim(rpn))) {
              errors.add(createValidationError("Fetch query [" + fqn
                  + "] predicate script variable: [@r." + rpn + "] not exists"));
            }
          }
        }
      }

      // check query parameter variables
      if (isNotEmpty(fieldNames)) {
        fq.getParameters().forEach(fp -> {
          if ((fp.getSource() == FetchQueryParameterSource.R)
              && !fieldNames.contains(fp.getSourceNamePath()[0])) {
            errors.add(createValidationError("Fetch query [" + fqn + "] parameter: ["
                + fp.getSourceNamePath()[0] + "] not exists"));
          }
        });
      }

      // check JSE injection script variables
      if (fq.getInjectionScript().isValid()
          && fq.getInjectionScript().getType() == ScriptType.JSE) {

        String injectCode = fq.getInjectionScript().getCode();

        // check parent query result variables
        if (isNotEmpty(fieldNames)) {
          List<String> parentResultParamNames = resolveJSEResultVariableNames(injectCode, false);
          if (isNotEmpty(parentResultParamNames)) {
            for (String rpn : parentResultParamNames) {
              if (!fieldNames.contains(trim(rpn))) {
                errors.add(createValidationError("Fetch query [" + fqn
                    + "] injection script variable: [@r." + rpn + "] not exists"));
              }
            }
          }
        }
        // check fetch query result variables
        if (isNotEmpty(fetchQueryFieldNames)) {
          List<String> fetchResultParamNames = resolveJSEResultVariableNames(injectCode, true);
          if (isNotEmpty(fetchResultParamNames)) {
            for (String frpn : fetchResultParamNames) {
              if (!fetchQueryFieldNames.contains(trim(frpn))) {
                errors.add(createValidationError("Fetch query [" + fqn
                    + "] injection script variable: [@fr." + frpn + "] not exists"));
              }
            }
          }
        }
        // check projection names
        Set<String> projectFieldNames = new LinkedHashSet<>();
        try {
          Map<Object, Object> map = ObjectMappers.mapReader().readValue(injectCode);
          Map<String, Object> projectionMap =
              getMapMap(map, JsonExpressionScriptProcessor.PROJECTION_KEY);
          if (isNotEmpty(projectionMap) && isNotEmpty(fetchQueryFieldNames)) {
            for (String frpn : projectionMap.keySet()) {
              if (!fetchQueryFieldNames.contains(trim(frpn))) {
                errors.add(createValidationError("Fetch query [" + fqn
                    + "] injection script projection name: [" + frpn + "] not exists"));
              } else {
                Object projectValue = projectionMap.get(frpn);
                if (projectValue instanceof Map<?, ?> pm) {
                  String rename =
                      getMapString(pm, JsonExpressionScriptProcessor.PROJECTION_RENAME_KEY);
                  if (rename != null) {
                    projectFieldNames.add(rename);
                  }
                } else if (toBoolean(projectValue)) {
                  projectFieldNames.add(frpn);
                }
              }
            }
          }
        } catch (JsonProcessingException e) {
          errors.add(createValidationError("Fetch query injection script process error!"));
        }
        if (isNotBlank(fq.getInjectPropertyName())) {
          // FIXME name path
          injectFieldNames.add(fq.getInjectPropertyName());
        } else if (projectFieldNames.isEmpty() && isNotEmpty(fetchQueryFieldNames)) {
          injectFieldNames.addAll(fetchQueryFieldNames);
        } else {
          injectFieldNames.addAll(projectFieldNames);
        }
      }
      fieldNames.addAll(injectFieldNames);
    }

  }
}

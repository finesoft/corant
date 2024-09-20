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

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyConsumer;
import static org.corant.shared.util.Lists.defaultEmpty;
import static org.corant.shared.util.Maps.getMapMap;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.transform;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.trim;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.Corant;
import org.corant.config.CorantConfigResolver;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.kernel.util.CommandLine;
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
import org.corant.modules.query.shared.spi.ResultAggregationHintHandler;
import org.corant.modules.query.shared.spi.ResultFieldConvertHintHandler;
import org.corant.modules.query.shared.spi.ResultMapReduceHintHandler;
import org.corant.modules.query.sql.cdi.SqlNamedQueryServiceManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Immutable.ImmutableSetBuilder;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import com.fasterxml.jackson.core.JsonProcessingException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
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
    protected String defaultVariableReplacement = "?";
    protected String defaultDirectiveReplacement = "";
    protected FeatureConfiguration featureConfiguration = new FeatureConfiguration();
    protected boolean includeMacro;
    protected boolean includeFetchQueryHandling;
    protected boolean includeMapReduceHints;
    protected boolean includeAggregationHints;
    protected boolean includeFieldConvertHints;
    protected boolean outputReplacedDirectiveStacks;
    protected boolean outputProcessedValidScript;
    protected boolean usingJSqlParser = false;
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
    public FreemarkerQueryScriptValidator includeAggregationHints(boolean includeAggregationHints) {
      this.includeAggregationHints = includeAggregationHints;
      return this;
    }

    @Experimental
    public FreemarkerQueryScriptValidator includeFetchQueryHandling(
        boolean includeFetchQueryHandling) {
      this.includeFetchQueryHandling = includeFetchQueryHandling;
      return this;
    }

    @Experimental
    public FreemarkerQueryScriptValidator includeFieldConvertHints(
        boolean includeFieldConvertHints) {
      this.includeFieldConvertHints = includeFieldConvertHints;
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

    @Experimental
    public FreemarkerQueryScriptValidator putDirectiveStackReplacement(String directiveStack,
        String replacement) {
      if (isNotBlank(directiveStack)) {
        directiveStacksReplacements.put(trim(directiveStack), trim(replacement));
      }
      return this;
    }

    @Experimental
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

    public void run() {
      try (Corant corant = prepare()) {
        validate();
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    public FreemarkerQueryScriptValidator usingJSqlParser(boolean usingJSqlParser) {
      this.usingJSqlParser = usingJSqlParser;
      return this;
    }

    public void validate() {
      final QueryMappingService service = resolve(QueryMappingService.class);

      Map<String, List<ValidationError>> errorMaps = new LinkedHashMap<>();
      Map<String, Set<String>> queryFieldNames = new LinkedHashMap<>();
      Set<String> validatedQueries = new HashSet<>();
      int size = service.getQueries().size();
      validatingInfoHandler.accept(format("Validating %s queries", size));
      for (Query query : service.getQueries()) {
        if (!validatedQueries.contains(query.getVersionedName())) {
          validateQuery(errorMaps, queryFieldNames, query, validatedQueries, size);
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

    }

    public void validateQuery(Map<String, List<ValidationError>> errorMaps,
        Map<String, Set<String>> queryFieldNames, Query query, Set<String> validatedQueries,
        int total) {

      final QueryMappingService service = resolve(QueryMappingService.class);
      final SqlNamedQueryServiceManager sqlQueryService =
          resolve(SqlNamedQueryServiceManager.class);
      final DataSourceService dataSources = resolve(DataSourceService.class);
      final ResultMapReduceHintHandler mapReduceHintHandler =
          resolve(ResultMapReduceHintHandler.class);
      final ResultAggregationHintHandler aggregationHintHandler =
          resolve(ResultAggregationHintHandler.class);
      final ResultFieldConvertHintHandler fieldConvertHintHandler =
          resolve(ResultFieldConvertHintHandler.class);

      final String queryName = query.getVersionedName();
      if (query.getScript().getType() != ScriptType.FM || resolveQueryType(query) != QueryType.SQL
          || skipQueryQualifier.contains(query.getQualifier()) || queryNameSkipper.test(queryName)
          || !queryNameFilter.test(queryName)) {
        validatedQueries.add(queryName);
        validatingInfoHandler
            .accept(format("[SKIP]: %s, [%s/%s]", queryName, validatedQueries.size(), total));
        return;
      }

      List<ValidationError> errors = errorMaps.computeIfAbsent(queryName, k1 -> new ArrayList<>());

      String script;
      try {
        script = getScript(query);
      } catch (Exception e) {
        errors.add(createValidationError("Parse script occurred error!", e));
        errors.add(createValidationError("[INVALID]: " + queryName + " script extract error!"));
        validatedQueries.add(queryName);
        validatingInfoHandler.accept(format("[INVALID]: %s script extract error, [%s/%s]",
            queryName, validatedQueries.size(), total));
        return;
      }
      Pair<DBMS, String> dss =
          sqlQueryService.resolveDataSourceSchema(resolveQueryQualifier(query));
      String dsName = dss.right();

      try (Connection conn = dataSources.resolve(dsName).getConnection()) {
        boolean setAutoCommit = false;
        if (conn.getAutoCommit()) {
          conn.setAutoCommit(false);
          setAutoCommit = true;
        }
        Pair<List<ValidationError>, Set<String>> results =
            doJSqlValidateAndGetFieldNames(queryName, script, dss, conn);
        if (isNotEmpty(results.left())) {
          errors.addAll(results.left());
          return;
        }
        Set<String> fieldNames = new LinkedHashSet<>(results.right());
        // validate fetch queries if necessary
        if (isNotEmpty(query.getFetchQueries()) && includeFetchQueryHandling) {
          for (FetchQuery fetchQuery : query.getFetchQueries()) {
            final String fetchQueryName = fetchQuery.getQueryReference().getVersionedName();
            Query usedFetchQuery = service.getQuery(fetchQueryName);
            if (canValidateFetchQuery(query, fetchQuery, usedFetchQuery)) {
              if (!validatedQueries.contains(fetchQueryName)) {
                validateQuery(errorMaps, queryFieldNames, usedFetchQuery, validatedQueries, total);
              }
              Set<String> fetchQueryFieldNames =
                  queryFieldNames.getOrDefault(fetchQueryName, new LinkedHashSet<>());
              validateFetchQuery(fieldNames, errors, fetchQuery, fetchQueryFieldNames);
            } else {
              validatingInfoHandler.accept(
                  "[SKIP]: fetch query " + fetchQuery.getQueryReference().getVersionedName());
            }
          }
        }
        if (isNotEmpty(query.getHints())) {
          for (QueryHint qh : query.getHints()) {
            // validate map reduce query hints if necessary
            validateMapReduceHintsIfNecessary(qh, mapReduceHintHandler, query, errors, fieldNames);
            // validate aggregation query hints if necessary
            validateAggregationHintsIfNecessary(qh, aggregationHintHandler, query, errors,
                fieldNames);
            // validate field convert query hints if necessary
            validateFieldConvertHintsIfNecessary(qh, fieldConvertHintHandler, query, errors,
                fieldNames);
          }
        }

        queryFieldNames.put(queryName, fieldNames);
        if (setAutoCommit) {
          conn.rollback();
          conn.setAutoCommit(true);
        }
      } catch (Exception ex) {
        errors.add(createValidationError("Validation occurred error!", ex));
      } finally {
        validatedQueries.add(queryName);
        if (isEmpty(errors)) {
          validatingInfoHandler
              .accept(format("[VALID]: %s, [%s/%s]", queryName, validatedQueries.size(), total));
          if (outputProcessedValidScript) {
            validatingInfoHandler.accept("[SCRIPT]");
            validatingInfoHandler.accept(script);
          }
        } else {
          validatingInfoHandler.accept(format("[INVALID]: %s [%s] ERRORS, [%s/%s]", queryName,
              errors.size(), validatedQueries.size(), total));
        }
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

    protected Pair<List<ValidationError>, Set<String>> doJSqlValidateAndGetFieldNames(
        final String queryName, String script, Pair<DBMS, String> dss, Connection conn) {
      Set<String> fieldNames = new LinkedHashSet<>();
      List<ValidationError> errors = new ArrayList<>();
      if (usingJSqlParser) {
        Validation validation =
            new Validation(featureConfiguration,
                Arrays.asList(resolveDatabaseType(dss.left()),
                    new JdbcDatabaseMetaDataCapability(conn, NamesLookup.NO_TRANSFORMATION)),
                script);
        // validate query self
        if (isNotEmpty(validation.validate())) {
          errors.addAll(validation.getErrors());
        } else {
          // collect the select field names
          Set<String> resolvedFieldNames =
              new LinkedHashSet<>(defaultEmpty(resolveFieldNames(validation),
                  () -> doSqlValidateAndGetFieldNames(conn, queryName, script, errors)));
          fieldNames.addAll(resolvedFieldNames);
        }
      } else {
        Set<String> resolvedFieldNames =
            new LinkedHashSet<>(doSqlValidateAndGetFieldNames(conn, queryName, script, errors));
        fieldNames.addAll(resolvedFieldNames);
      }
      return Pair.of(errors, fieldNames);
    }

    protected List<String> doSqlValidateAndGetFieldNames(Connection conn, String queryName,
        String sql, List<ValidationError> errors) {
      List<String> fieldNames = new ArrayList<>();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ResultSetMetaData metaData = ps.getMetaData();
        if (metaData == null || metaData.getColumnCount() < 1) {
          ValidationError err = new ValidationError(sql);
          err.addError(new ValidationException("Can't find any column"));
          errors.add(err);
        }
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          fieldNames.add(metaData.getColumnLabel(i));
        }
      } catch (Exception ex) {
        fieldNames.clear();
        ValidationError err = new ValidationError(sql);
        err.addError(new ValidationException(ex));
        errors.add(err);
      }
      return fieldNames;
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

    protected Corant prepare() {
      LoggerFactory.disableAccessWarnings();
      LoggerFactory.disableLogger();
      Systems.setProperty("corant.query.verify-deployment", "true");
      CorantConfigResolver.adjust("corant.webserver.auto-start", "false",
          "corant.flyway.migrate.enable", "false", "corant.jta.transaction.auto-recovery", "false");
      return Corant.startup(SqlQueryDeveloperKits.class,
          new String[] {Corant.DISABLE_BOOST_LINE_CMD,
              new CommandLine(Corant.DISABLE_BEFORE_START_HANDLER_CMD,
                  "org.corant.modules.logging.Log4jProvider").toString()});
    }

    protected String replaceDirectiveStacksNecessary(Query query, String string) {
      if (isEmpty(directiveStacksReplacements)) {
        return string;
      }

      List<String> lines = string.lines().toList();

      List<LineReplacement> collectedRepLns = new ArrayList<>();
      directiveStacksReplacements.forEach((k, v) -> {
        List<LineReplacement> tmp = resolveReplaceDirectiveStacksLines(k, v, lines);
        if (isNotEmpty(tmp)) {
          for (LineReplacement tmpLs : tmp) {
            if (isNotEmpty(tmpLs.lineNos)) {
              collectedRepLns.add(tmpLs);
            }
          }
        }
      });

      if (isEmpty(collectedRepLns)) {
        return string;
      }

      // sort the replacement line numbers
      Collections.sort(collectedRepLns);

      // remove the intersect line numbers
      List<LineReplacement> repLns = new ArrayList<>();
      List<Integer> pre = null;
      for (LineReplacement tmp : collectedRepLns) {
        if (pre == null || pre.stream().noneMatch(tmp.lineNos::contains)) {
          repLns.add(tmp);
          pre = tmp.lineNos;
        }
      }
      if (repLns.isEmpty()) {
        return string;
      }

      String lineSpr = Systems.getLineSeparator();
      StringBuilder sb = new StringBuilder();
      StringBuilder re = new StringBuilder();
      int size = lines.size();
      LineReplacement matchLines = null;
      String matchReplacement = null;
      for (int i = 0; i < size; i++) {
        if (matchLines == null) {
          for (LineReplacement repLn : repLns) {
            if (repLn.lineNos.contains(i)) {
              matchLines = repLn;
              if (repLn.replacement instanceof String sr) {
                matchReplacement = sr;
              } else if (repLn.replacement instanceof Map<?, ?> mr) {
                matchReplacement = getMapString(mr, query.getVersionedName());
              }
              repLns.remove(repLn);
              break;
            }
          }
        }

        String line = lines.get(i);

        if (matchLines == null) {
          sb.append(line).append(lineSpr);
        } else {
          matchLines.lineNos.remove(Integer.valueOf(i));
          if (matchLines.lineNos.isEmpty()) {
            if (matchReplacement != null) {
              sb.append(matchReplacement).append(lineSpr);
            }
            if (outputReplacedDirectiveStacks) {
              re.append("[STACK-REPLACE]: \"").append(matchLines.matched)
                  .append("\" replaced by \"").append(defaultString(matchReplacement)).append("\"")
                  .append(lineSpr);
              re.append("|-").append(lineSpr);
              for (String rl : matchLines.lines) {
                re.append("|- ").append(rl).append(lineSpr);
              }
              re.append("-".repeat(100)).append(lineSpr);
            }
            matchLines = null;
          }
        }
      }
      if (outputReplacedDirectiveStacks && !re.isEmpty()) {
        validatingInfoHandler.accept(re.toString());
      }
      return sb.toString();
    }

    protected DatabaseType resolveDatabaseType(DBMS dbms) {
      DatabaseType dsType = DatabaseType.ANSI_SQL;
      switch (dbms) {
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
      return dsType;
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

    protected List<String> resolveFieldNames(Validation validation) {
      if (validation.getParsedStatements() != null
          && isNotEmpty(validation.getParsedStatements())) {
        for (Statement st : validation.getParsedStatements()) {
          if (st instanceof PlainSelect ps) {
            List<String> fieldNames = new ArrayList<>();
            for (SelectItem<?> si : ps.getSelectItems()) {
              Expression item = si.getExpression();
              if (item instanceof AllColumns) {
                fieldNames.clear();
                break;
              }
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

    // FIXME using char stream ??
    protected List<LineReplacement> resolveReplaceDirectiveStacksLines(String sd, Object rep,
        List<String> lines) {
      String usd = trim(sd);
      int s = usd.indexOf(' ');
      String start = usd.substring(0, s);
      String end = "</" + usd.substring(1, s) + ">";
      List<LineReplacement> poses = new ArrayList<>();
      List<Integer> stackPoses = new ArrayList<>();
      List<String> stackLines = new ArrayList<>();
      int lineNo = 0;
      int io = 0;
      // Stack<String> stack = new Stack<>();
      boolean inStack = false;
      for (String line : lines) {
        String tl = trim(line);
        if (inStack) {
          stackPoses.add(lineNo);
          stackLines.add(line);
          if (tl.startsWith(start) && !tl.endsWith("/>")) {
            // stack.push(tl);
            io++;
          } else if (tl.equals(end)) {
            // stack.pop();
            io--;
            if (io == 0) {
              poses.add(new LineReplacement(sd, rep, stackPoses, stackLines));
              inStack = false;
              stackPoses.clear();
              stackLines.clear();
            }
          }
        } else if (tl.equals(usd)) {
          io++;
          stackPoses.add(lineNo);
          stackLines.add(line);
          // stack.push(tl);
          inStack = true;
          if (tl.endsWith("/>")) {
            io = 0;
            // stack.clear();
            poses.add(new LineReplacement(sd, rep, stackPoses, stackLines));
            inStack = false;
            stackPoses.clear();
            stackLines.clear();
          }
        }
        lineNo++;
      }
      return poses;
    }

    protected void validateAggregationHintsIfNecessary(QueryHint qh,
        final ResultAggregationHintHandler aggregationHintHandler, Query query,
        List<ValidationError> errors, Set<String> fieldNames) {
      if (!includeAggregationHints) {
        return;
      }
      if (aggregationHintHandler.supports(Map.class, qh) && isNotEmpty(fieldNames)) {
        String aggName = aggregationHintHandler.resolveAggNames(qh);
        if (isBlank(aggName)) {
          return;
        }
        Pair<Boolean, Set<String>> pair = aggregationHintHandler.resolveAggFieldNames(qh);
        if (isEmpty(pair)) {
          return;
        }
        Set<String> appends = new LinkedHashSet<>();
        appends.add(aggName);
        pair.right().forEach(rf -> {
          if (!fieldNames.contains(rf)) {
            errors.add(createValidationError(
                format("Aggregation query hint aggregated field [%s] not exits, valid names: [%s]",
                    rf, join(",", fieldNames))));
          } else {
            appends.add(aggName.concat(".").concat(rf));
          }
        });
        if (pair.first()) {
          fieldNames.removeIf(f -> !pair.contains(f));
        } else {
          fieldNames.removeIf(pair::contains);
        }

        if (isNotEmpty(appends)) {
          fieldNames.addAll(appends);
        }
      }
    }

    protected void validateFetchQuery(Set<String> fieldNames, List<ValidationError> errors,
        FetchQuery fq, Set<String> fetchQueryFieldNames) {
      // check JSE predicate script variables
      String fqn = fq.getQueryReference().getVersionedName();
      if (isNotEmpty(fieldNames) && fq.getPredicateScript().isValid()
          && fq.getPredicateScript().getType() == ScriptType.JSE) {
        List<String> resultParamNames =
            resolveJSEResultVariableNames(fq.getPredicateScript().getCode(), false);
        if (isNotEmpty(resultParamNames)) {
          for (String rpn : resultParamNames) {
            if (!fieldNames.contains(trim(rpn))) {
              errors.add(createValidationError(format(
                  "Fetch query [%s] predicate script variable: [@r.%s] not exists, valid field names: [%s]",
                  fqn, rpn, join(", ", fieldNames))));
            }
          }
        }
      }

      // check query parameter variables
      if (isNotEmpty(fieldNames)) {
        fq.getParameters().forEach(fp -> {
          if ((fp.getSource() == FetchQueryParameterSource.R)
              && !fieldNames.contains(fp.getSourceName())) {
            errors.add(createValidationError(
                format("Fetch query [%s] parameter: [%s] not exists, valid names: [%s]", fqn,
                    fp.getSourceName(), join(", ", fieldNames))));
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
                errors.add(createValidationError(format(
                    "Fetch query [%s] injection script variable: [@r.%s] not exists, valid names: [%s]",
                    fqn, rpn, join(", ", fieldNames))));
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
                errors.add(createValidationError(format(
                    "Fetch query [%s] injection script variable: [@fr.%s] not exists, valid names: [%s]",
                    fqn, frpn, join(", ", fetchQueryFieldNames))));
              }
            }
          }
        }
        // check projection names
        Set<String> injectFieldNames = new LinkedHashSet<>();
        Set<String> projectFieldNames = new LinkedHashSet<>();
        try {
          Map<Object, Object> map = ObjectMappers.mapReader().readValue(injectCode);
          Map<String, Object> projectionMap =
              getMapMap(map, JsonExpressionScriptProcessor.PROJECTION_KEY);
          if (isNotEmpty(projectionMap) && isNotEmpty(fetchQueryFieldNames)) {
            for (String frpn : projectionMap.keySet()) {
              if (!fetchQueryFieldNames.contains(trim(frpn))) {
                errors.add(createValidationError(format(
                    "Fetch query [%s] injection script projection name: [%s] not exists, valid names: [%s]",
                    fqn, frpn, join(", ", fetchQueryFieldNames))));
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

        if (projectFieldNames.isEmpty() && isNotEmpty(fetchQueryFieldNames)) {
          projectFieldNames.addAll(fetchQueryFieldNames);
        }
        if (isNotBlank(fq.getInjectPropertyName())) {
          injectFieldNames.add(fq.getInjectPropertyName());
          for (String pfn : projectFieldNames) {
            injectFieldNames.add(fq.getInjectPropertyName().concat(".").concat(pfn));
          }
        } else {
          injectFieldNames.addAll(projectFieldNames);
        }
        fieldNames.addAll(injectFieldNames);
      }
    }

    protected void validateFieldConvertHintsIfNecessary(QueryHint qh,
        final ResultFieldConvertHintHandler fieldConvertHintHandler, Query query,
        List<ValidationError> errors, Set<String> fieldNames) {
      if (!includeFieldConvertHints) {
        return;
      }
      if (fieldConvertHintHandler.supports(Map.class, qh) && isNotEmpty(fieldNames)) {
        List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> list =
            fieldConvertHintHandler.resolveConversions(qh);
        if (isNotEmpty(list)) {
          list.forEach(p -> {
            String rf = String.join(".", p.first());
            if (!fieldNames.contains(rf)) {
              errors.add(createValidationError(
                  format("Field convert query hint field [%s] not exits, valid names: [%s]", rf,
                      join(", ", fieldNames))));
            }
          });
        }
      }
    }

    protected void validateMapReduceHintsIfNecessary(QueryHint qh,
        final ResultMapReduceHintHandler mapReduceHintHandler, Query query,
        List<ValidationError> errors, Set<String> fieldNames) {
      if (!includeMapReduceHints) {
        return;
      }
      if (mapReduceHintHandler.supports(Map.class, qh) && isNotEmpty(fieldNames)) {
        String mapName = mapReduceHintHandler.resolveMapFieldname(qh);
        if (isBlank(mapName)) {
          return;
        }
        List<Triple<String, String[], Class<?>>> list =
            mapReduceHintHandler.resolveReduceFields(qh);
        if (isEmpty(list)) {
          return;
        }
        Set<String> removes = new LinkedHashSet<>();
        Set<String> appends = new LinkedHashSet<>();
        appends.add(mapName);
        boolean remain = mapReduceHintHandler.resolveRetainFields(qh);
        list.forEach(x -> {
          String rf = String.join(".", x.getMiddle());
          if (!remain) {
            removes.add(rf);
          }
          if (!fieldNames.contains(rf)) {
            errors.add(createValidationError(
                format("Map reduce query hint reduce field [%s] not exits, valid names: [%s]", rf,
                    join(", ", fieldNames))));
          } else {
            appends.add(mapName.concat(".").concat(x.left()));
          }
        });
        if (isNotEmpty(removes)) {
          fieldNames.removeAll(removes);
        }
        if (isNotEmpty(appends)) {
          fieldNames.addAll(appends);
        }
      }
    }

    /**
     * corant-modules-query-sql
     *
     * @author bingo 09:58:35
     */
    protected static class LineReplacement implements Comparable<LineReplacement> {
      final String matched;
      final Object replacement;
      final List<Integer> lineNos = new ArrayList<>();
      final List<String> lines = new ArrayList<>();

      LineReplacement(String matched, Object replacement, List<Integer> lineNos,
          List<String> lines) {
        this.matched = matched;
        this.replacement = replacement;
        if (lineNos != null) {
          this.lineNos.addAll(lineNos);
        }
        if (lines != null) {
          this.lines.addAll(lines);
        }
      }

      @Override
      public int compareTo(LineReplacement o) {
        return lineNos.get(0).compareTo(o.lineNos.get(0));
      }

    }
  }
}

/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.datasource.shared;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.defaultBlank;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.substring;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Objects;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.util.deparser.DeleteDeParser;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.InsertDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.UpdateDeParser;
import net.sf.jsqlparser.util.deparser.UpsertDeParser;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午4:06:43
 */
public class SqlStatements {

  /**
   * Converts the given named parameters(:name) prepared SQL statement and the given query
   * parameters maps to a normalized SQL prepared statement and its query parameters and return
   * them, the returned query parameters array correspond to the placeholders (?) in the returned
   * SQL statement one to one. Throws exception if the name of query parameter not find in the given
   * named parameter maps.
   *
   * <p>
   * Note: If the given parameter type is an array or a collection, the number of placeholders
   * ({@code ?}) in the returned SQL corresponding to the parameter will automatically change to the
   * number of elements of the array or collection. <br>
   * The name of the parameter can't be a number
   * </p>
   *
   * <pre>
   * Examples:
   *
   * <b>ORIGINAL GIVEN SQL:</b> SELECT * FROM t_person WHERE name = :mName AND id IN (:mIds)
   * <b>PARAMETER MAPS:</b> Map.of("mName","bingo","mIds",List.of(1,2,3,4))
   *
   * <b>RETURNED SQL:</b> SELECT * FROM t_person WHERE name = ? AND id IN (?,?,?,?)
   * <b>RETURNED PARAMETERS:</b> ["bingo",1,2,3,4]
   * </pre>
   *
   * @param sql the SQL statement containing named query parameters
   * @param namedParameters the named query parameter maps
   * @return a normalized SQL query statement and an ordered array of query parameters.
   */
  public static Pair<String, Object[]> normalize(String sql, Map<String, Object> namedParameters) {
    try {
      StringBuilder statement = new StringBuilder();
      List<Object> useParams = new ArrayList<>();
      ExpressionDeParser expressionDeParser =
          new JdbcParameterExpressionDeParser(namedParameters, useParams);
      normalize(parse(sql, true), statement, expressionDeParser);
      return Pair.of(statement.toString(), useParams.toArray());
    } catch (JSQLParserException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  /**
   * Converts the given containing parameters placeholders (?) prepared SQL statement and the given
   * query parameters array to a normalized SQL prepared statement and its query parameters and
   * return them, the returned query parameters array correspond to the placeholders (?) in the
   * returned SQL statement one to one. Throws exception if the number of parameters placeholders
   * less then the length of the given query parameters array.
   *
   * <p>
   * Note: If the given parameter type is an array or a collection, the number of placeholders
   * ({@code ?}) in the returned SQL corresponding to the parameter will automatically change to the
   * number of elements of the array or collection.
   * </p>
   *
   * <pre>
   * Examples:
   *
   * <b>ORIGINAL GIVEN SQL:</b> SELECT * FROM t_person WHERE name = ? AND id IN (?)
   * <b>PARAMETERS ARRAY:</b> ["bingo", List.of(1,2,3,4)]
   *
   * <b>RETURNED SQL:</b> SELECT * FROM t_person WHERE name = ? AND id IN (?,?,?,?)
   * <b>RETURNED PARAMETERS:</b> ["bingo",1,2,3,4]
   * </pre>
   *
   * @param sql the SQL containing parameters placeholders ({@code ?})
   * @param ordinaryParameters the query parameters array
   * @return a normalized SQL query statement and an ordered array of query parameters.
   */
  public static Pair<String, Object[]> normalize(String sql, Object... ordinaryParameters) {
    if (isBlank(sql) || ordinaryParameters.length == 0
        || streamOf(ordinaryParameters).noneMatch(Collection.class::isInstance)) {
      return Pair.of(sql, ordinaryParameters);
    }
    try {
      StringBuilder statement = new StringBuilder();
      List<Object> useParams = new ArrayList<>();
      ExpressionDeParser expressionDeParser =
          new JdbcParameterExpressionDeParser(ordinaryParameters, useParams);
      normalize(parse(sql, true), statement, expressionDeParser);
      return Pair.of(statement.toString(), useParams.toArray());
    } catch (JSQLParserException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static String normalizeJdbcParameterPlaceHolder(String sql) {
    if (isBlank(sql) || (!sql.contains(":") && !sql.contains("?"))) {
      return sql;
    }
    try {
      StringBuilder statement = new StringBuilder();
      NormalizeJdbcParameterExpressionDeParser expressionDeParser =
          new NormalizeJdbcParameterExpressionDeParser();
      normalize(parse(sql, true), statement, expressionDeParser);
      if (expressionDeParser.indexes != null) {
        int size = expressionDeParser.indexes.size();
        for (int i = 1; i <= size; i++) {
          if (!expressionDeParser.indexes.contains(i)) {
            throw new JSQLParserException(format("SQL parameter place hoder index: %s error", i));
          }
        }
      }
      return statement.toString();
    } catch (JSQLParserException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static Statement parse(String sql, boolean allowComplex) throws JSQLParserException {
    if (allowComplex) {
      return CCJSqlParserUtil.parse(sql, CCJSqlParser::withComplexParsing);
    } else {
      return CCJSqlParserUtil.parse(sql);
    }
  }

  /**
   * Returns a new reorganization a count statement based on the given select statement, the given
   * select statement may be optimized, such as removing order by clauses, removing the select
   * fields, etc.
   *
   * @param sql the original select statement
   * @param countColumnExpression the count column expression
   * @param wrappedTableAlias the wrapped select statement table alias
   * @param aggregateFunctions the aggregation functions that can't be optimized.
   * @return an optimized count query statement
   * @throws Exception if the given sql can't be resolved or occurred error
   */
  public static String resolveCountSql(String sql, String countColumnExpression,
      String wrappedTableAlias, Collection<String> aggregateFunctions) throws Exception {
    Statement statement = parse(sql, true);
    Select select = shouldInstanceOf(statement, Select.class);
    if (!(select instanceof PlainSelect)) {
      return resolveSimpleCountSql(select, countColumnExpression, wrappedTableAlias);
    }
    PlainSelect plainSelect = (PlainSelect) statement;
    // handle order by clauses
    if (!containsParameterizedOrderbyElement(plainSelect.getOrderByElements())) {
      plainSelect.setOrderByElements(null);
    }
    // handle count select items
    if (!canBeOptimized(plainSelect, aggregateFunctions)) {
      return resolveSimpleCountSql(plainSelect, countColumnExpression, wrappedTableAlias);
    }
    plainSelect.setSelectItems(singletonList(new SelectItem<>(new Column(countColumnExpression))));
    return plainSelect.toString();
  }

  /**
   * Returns a new reorganization a count statement based on the given select statement, the given
   * select statement may be optimized, such as removing order by clauses, removing the select
   * fields, etc.
   *
   * @param sql the original select statement
   * @param countColumn the count field name
   * @param countColumnAlias the count field alias
   * @param aggregateFunctions the aggregation functions that can't be optimized.
   * @param wrappedTableAlias the wrapped select statement table alias
   * @return an optimized count query statement
   * @throws Exception if the given sql can't be resolved or occurred error
   */
  public static String resolveCountSql(String sql, String countColumn, String countColumnAlias,
      String wrappedTableAlias, Collection<String> aggregateFunctions) throws Exception {
    return resolveCountSql(sql,
        "COUNT(" + defaultBlank(countColumn, "1") + ") " + defaultString(countColumnAlias),
        wrappedTableAlias, aggregateFunctions);
  }

  static boolean canBeOptimized(Select select, Collection<String> aggregationFunctions) {
    if (select instanceof PlainSelect plainSelect) {
      // check whether contains group by/distinct/top/having
      if (plainSelect.getGroupBy() != null || plainSelect.getDistinct() != null
          || plainSelect.getTop() != null || plainSelect.getHaving() != null
          || plainSelect.getFromItem().getPivot() != null) {
        return false;
      }
      for (SelectItem<?> si : plainSelect.getSelectItems()) {
        // check whether select item contains variable placeholder
        if (si.toString().indexOf('?') != -1) {
          return false;
        }
        // check whether select item is expression (sub-query / function)
        Expression sie = si.getExpression();
        if (sie instanceof Function) {
          if (isEmpty(aggregationFunctions)) {
            return false;
          } else {
            String funcName = ((Function) sie).getName().toUpperCase(Locale.ROOT);
            for (String af : aggregationFunctions) {
              if (funcName.startsWith(af)) {
                return false;
              }
            }
          }
        } else if (sie instanceof ParenthesedExpressionList && si.getAlias() != null) {
          return false;
        }
      }
    }
    return true;
  }

  static boolean containsParameterizedOrderbyElement(List<OrderByElement> orderbyElements) {
    return orderbyElements != null
        && orderbyElements.stream().anyMatch(e -> e.toString().indexOf('?') != -1);
  }

  static void normalize(Statement statement, StringBuilder buffer,
      ExpressionDeParser expressionDeParser) {
    if (statement instanceof Select select) {
      SelectDeParser deparser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(deparser);
      expressionDeParser.setBuffer(buffer);
      if (isNotEmpty(select.getWithItemsList())) {
        buffer.append("WITH ");
        for (Iterator<WithItem> iter = select.getWithItemsList().iterator(); iter.hasNext();) {
          WithItem withItem = iter.next();
          buffer.append(withItem);
          if (iter.hasNext()) {
            buffer.append(",");
          }
          buffer.append(" ");
        }
      }
      select.accept(deparser, null);
    } else if (statement instanceof Delete delete) {
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      DeleteDeParser deleteDeParser = new DeleteDeParser(expressionDeParser, buffer);
      expressionDeParser.setBuffer(buffer);
      deleteDeParser.deParse(delete);
    } else if (statement instanceof Insert insert) {
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      InsertDeParser insertDeParser =
          new InsertDeParser(expressionDeParser, selectDeParser, buffer);
      expressionDeParser.setBuffer(buffer);
      insertDeParser.deParse(insert);
    } else if (statement instanceof Update update) {
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      UpdateDeParser updateDeParser = new UpdateDeParser(expressionDeParser, buffer);
      expressionDeParser.setBuffer(buffer);
      updateDeParser.deParse(update);
    } else if (statement instanceof Upsert upsert) {
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      UpsertDeParser upsertDeParser =
          new UpsertDeParser(expressionDeParser, selectDeParser, buffer);
      expressionDeParser.setBuffer(buffer);
      upsertDeParser.deParse(upsert);
    } else {
      throw new NotSupportedException("Only supports SELECT/UPDATE/INSERT/DELETE statements!");
    }
  }

  static String resolveSimpleCountSql(Select select, String countColumnExpression,
      String wrappedTableAlias) {
    PlainSelect plainSelect = new PlainSelect();
    ParenthesedSelect parenthesedSelect = new ParenthesedSelect();
    parenthesedSelect.setSelect(select);
    if (wrappedTableAlias != null) {
      parenthesedSelect.setAlias(new Alias(wrappedTableAlias, false));
    }
    plainSelect.setFromItem(parenthesedSelect);
    plainSelect.setSelectItems(singletonList(new SelectItem<>(new Column(countColumnExpression))));
    return plainSelect.toString();
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 上午10:17:45
   */
  protected static class JdbcParameterExpressionDeParser extends ExpressionDeParser {
    protected final Object[] ordinaryParameters;
    protected final Map<String, Object> namedParameters;
    protected final List<Object> useParams;

    protected JdbcParameterExpressionDeParser(Map<String, Object> parameters,
        List<Object> useParams) {
      namedParameters = parameters;
      ordinaryParameters = Objects.EMPTY_ARRAY;
      this.useParams = useParams;
    }

    protected JdbcParameterExpressionDeParser(Object[] parameters, List<Object> useParams) {
      ordinaryParameters = parameters;
      namedParameters = null;
      this.useParams = useParams;
    }

    @Override
    public <S> StringBuilder visit(JdbcNamedParameter jdbcParameter, S context) {
      String name = jdbcParameter.getName();
      shouldBeTrue(namedParameters != null && namedParameters.containsKey(name),
          "The named parameter [%s] in SQL does not match the given parameter!", name);
      Object parameter = namedParameters.get(name);
      List<Object> tempParams = resolveParameter(parameter);
      int size = sizeOf(tempParams);
      if (size > 1) {
        buffer.append(substring("?,".repeat(size), 0, -1));
      } else {
        buffer.append("?");
      }
      useParams.addAll(tempParams);
      return buffer;
    }

    @Override
    public <S> StringBuilder visit(JdbcParameter jdbcParameter, S context) {
      // if (jdbcParameter.isUseFixedIndex()) {
      // return super.visit(jdbcParameter,context);
      // }
      if (jdbcParameter.getIndex() > ordinaryParameters.length) {
        throw new CorantRuntimeException("SQL placeholder does not match the given parameter!");
      }
      Object parameter = ordinaryParameters[jdbcParameter.getIndex() - 1];
      List<Object> tempParams = resolveParameter(parameter);
      int size = sizeOf(tempParams);
      if (size > 1) {
        buffer.append(substring("?,".repeat(size), 0, -1));
      } else {
        buffer.append("?");
      }
      useParams.addAll(tempParams);
      return buffer;
    }

    protected List<Object> resolveParameter(Object parameter) {
      List<Object> params = new ArrayList<>();
      if (parameter instanceof Collection) {
        params.addAll((Collection<?>) parameter);
      } else if (parameter != null && parameter.getClass().isArray()) {
        Collections.addAll(params, wrapArray(parameter));
      } else {
        params.add(parameter);
      }
      return params;
    }
  }

  /**
   * corant-modules-datasource-shared
   *
   * @author bingo 10:19:36
   */
  protected static class NormalizeJdbcParameterExpressionDeParser extends ExpressionDeParser {

    protected Set<Integer> indexes = null;

    @Override
    public <S> StringBuilder visit(JdbcNamedParameter jdbcParameter, S context) {
      shouldBeNull(indexes, "A sql can't contains named and indexed parameters");
      buffer.append("?");
      return buffer;
    }

    @Override
    public <S> StringBuilder visit(JdbcParameter jdbcParameter, S context) {
      if (jdbcParameter.isUseFixedIndex()) {
        if (indexes == null) {
          indexes = new LinkedHashSet<>();
        }
        indexes.add(jdbcParameter.getIndex());
      } else {
        shouldBeNull(indexes, "A sql can't contains non-indexed and indexed parameters");
      }
      buffer.append("?");
      return buffer;
    }
  }
}

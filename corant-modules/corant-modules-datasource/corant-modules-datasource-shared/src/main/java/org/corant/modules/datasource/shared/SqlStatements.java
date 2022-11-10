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

import static java.util.Collections.singletonList;
import static org.corant.shared.util.Assertions.shouldBeTrue;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.deparser.DeleteDeParser;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.InsertDeParser;
import net.sf.jsqlparser.util.deparser.ReplaceDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.UpdateDeParser;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午4:06:43
 *
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
      normalize(CCJSqlParserUtil.parse(sql), statement, expressionDeParser);
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
        || streamOf(ordinaryParameters).noneMatch(p -> p instanceof Collection)) {
      return Pair.of(sql, ordinaryParameters);
    }
    try {
      StringBuilder statement = new StringBuilder();
      List<Object> useParams = new ArrayList<>();
      ExpressionDeParser expressionDeParser =
          new JdbcParameterExpressionDeParser(ordinaryParameters, useParams);
      normalize(CCJSqlParserUtil.parse(sql), statement, expressionDeParser);
      return Pair.of(statement.toString(), useParams.toArray());
    } catch (JSQLParserException ex) {
      throw new CorantRuntimeException(ex);
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
   * @throws JSQLParserException if CCJSqlParserUtil occurred error
   */
  public static String resolveCountSql(String sql, String countColumnExpression,
      String wrappedTableAlias, Collection<String> aggregateFunctions) throws JSQLParserException {
    Select select = (Select) CCJSqlParserUtil.parse(sql);
    SelectBody selectBody = select.getSelectBody();
    reviseCountSqlSelectBody(selectBody);
    if (isNotEmpty(select.getWithItemsList())) {
      for (WithItem item : select.getWithItemsList()) {
        if (item.getSubSelect() != null) {
          reviseCountSqlSelectBody(item.getSubSelect().getSelectBody());
        }
      }
    }
    final SelectExpressionItem countItem =
        new SelectExpressionItem(new Column(countColumnExpression));
    final List<SelectItem> countItems = singletonList(countItem);
    if (canBeOptimized(selectBody, aggregateFunctions)) {
      ((PlainSelect) selectBody).setSelectItems(countItems);
    } else {
      PlainSelect plainSelect = new PlainSelect();
      SubSelect subSelect = new SubSelect();
      subSelect.setSelectBody(selectBody);
      if (wrappedTableAlias != null) {
        subSelect.setAlias(new Alias(wrappedTableAlias, false));
      }
      plainSelect.setFromItem(subSelect);
      plainSelect.setSelectItems(countItems);
      select.setSelectBody(plainSelect);
    }
    return select.toString();
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
   * @throws JSQLParserException if CCJSqlParserUtil occurred error
   */
  public static String resolveCountSql(String sql, String countColumn, String countColumnAlias,
      String wrappedTableAlias, Collection<String> aggregateFunctions) throws JSQLParserException {
    return resolveCountSql(sql,
        "COUNT(" + defaultBlank(countColumn, "1") + ") " + defaultString(countColumnAlias),
        wrappedTableAlias, aggregateFunctions);
  }

  static boolean canBeOptimized(SelectBody selectBody, Collection<String> aggregationFunctions) {
    if (selectBody instanceof PlainSelect) {
      PlainSelect plainSelect = (PlainSelect) selectBody;
      if (plainSelect.getGroupBy() != null || plainSelect.getDistinct() != null
          || plainSelect.getHaving() != null) {
        return false;
      }
      for (SelectItem it : plainSelect.getSelectItems()) {
        // check whether select item contains variable place holder
        if (it.toString().indexOf('?') != -1) {
          return false;
        }
        // check whether select item is expression (sub-query / function)
        if (it instanceof SelectExpressionItem) {
          Expression itEx = ((SelectExpressionItem) it).getExpression();
          if (itEx instanceof Function) {
            if (isEmpty(aggregationFunctions)) {
              return false;
            } else {
              String funcName = ((Function) itEx).getName().toUpperCase(Locale.ROOT);
              for (String af : aggregationFunctions) {
                if (funcName.startsWith(af)) {
                  return false;
                }
              }
            }
          } else if (itEx instanceof Parenthesis
              && ((SelectExpressionItem) it).getAlias() != null) {
            return false;
          }
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
    if (statement instanceof Select) {
      Select select = (Select) statement;
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
      select.getSelectBody().accept(deparser);
    } else if (statement instanceof Delete) {
      Delete delete = (Delete) statement;
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      expressionDeParser.setBuffer(buffer);
      DeleteDeParser deleteDeParser = new DeleteDeParser(expressionDeParser, buffer);
      deleteDeParser.deParse(delete);
    } else if (statement instanceof Insert) {
      Insert insert = (Insert) statement;
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      expressionDeParser.setBuffer(buffer);
      InsertDeParser insertDeParser =
          new InsertDeParser(expressionDeParser, selectDeParser, buffer);
      insertDeParser.deParse(insert);
    } else if (statement instanceof Update) {
      Update update = (Update) statement;
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      expressionDeParser.setBuffer(buffer);
      UpdateDeParser updateDeParser = new UpdateDeParser(expressionDeParser, buffer);
      updateDeParser.deParse(update);
    } else if (statement instanceof Replace) {
      Replace replace = (Replace) statement;
      SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, buffer);
      expressionDeParser.setSelectVisitor(selectDeParser);
      expressionDeParser.setBuffer(buffer);
      ReplaceDeParser replaceDeParser =
          new ReplaceDeParser(expressionDeParser, selectDeParser, buffer);
      replaceDeParser.deParse(replace);
    } else {
      throw new NotSupportedException("Only supports SELECT/UPDATE/INSERT/DELETE statemens!");
    }
  }

  static void reviseCountSqlFromItem(FromItem fromItem) {
    if (fromItem instanceof SubJoin) {
      SubJoin subJoin = (SubJoin) fromItem;
      if (subJoin.getJoinList() != null && subJoin.getJoinList().size() > 0) {
        for (Join join : subJoin.getJoinList()) {
          if (join.getRightItem() != null) {
            reviseCountSqlFromItem(join.getRightItem());
          }
        }
      }
      if (subJoin.getLeft() != null) {
        reviseCountSqlFromItem(subJoin.getLeft());
      }
    } else if (fromItem instanceof SubSelect) {
      SubSelect subSelect = (SubSelect) fromItem;
      if (subSelect.getSelectBody() != null) {
        reviseCountSqlSelectBody(subSelect.getSelectBody());
      }
    } else if (fromItem instanceof LateralSubSelect) {
      LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
      if (lateralSubSelect.getSubSelect() != null) {
        SubSelect subSelect = lateralSubSelect.getSubSelect();
        if (subSelect.getSelectBody() != null) {
          reviseCountSqlSelectBody(subSelect.getSelectBody());
        }
      }
    }
  }

  static void reviseCountSqlPlainSelect(PlainSelect plainSelect) {
    if (!containsParameterizedOrderbyElement(plainSelect.getOrderByElements())) {
      plainSelect.setOrderByElements(null);
    }
    if (plainSelect.getFromItem() != null) {
      reviseCountSqlFromItem(plainSelect.getFromItem());
    }
    if (plainSelect.getJoins() != null && plainSelect.getJoins().size() > 0) {
      List<Join> joins = plainSelect.getJoins();
      for (Join join : joins) {
        if (join.getRightItem() != null) {
          reviseCountSqlFromItem(join.getRightItem());
        }
      }
    }
  }

  static void reviseCountSqlSelectBody(SelectBody selectBody) {
    if (selectBody != null) {
      if (selectBody instanceof PlainSelect) {
        reviseCountSqlPlainSelect((PlainSelect) selectBody);
      } else if (selectBody instanceof WithItem) {
        WithItem withItem = (WithItem) selectBody;
        if (withItem.getSubSelect() != null) {
          reviseCountSqlSelectBody(withItem.getSubSelect().getSelectBody());
        }
      } else {
        SetOperationList operationList = (SetOperationList) selectBody;
        if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
          List<SelectBody> plainSelects = operationList.getSelects();
          for (SelectBody plainSelect : plainSelects) {
            reviseCountSqlSelectBody(plainSelect);
          }
        }
        if (!containsParameterizedOrderbyElement(operationList.getOrderByElements())) {
          operationList.setOrderByElements(null);
        }
      }
    }
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 上午10:17:45
   *
   */
  static class JdbcParameterExpressionDeParser extends ExpressionDeParser {
    private final Object[] ordinaryParameters;
    private final Map<String, Object> namedParameters;
    private final List<Object> useParams;

    JdbcParameterExpressionDeParser(Map<String, Object> parameters, List<Object> useParams) {
      namedParameters = parameters;
      ordinaryParameters = Objects.EMPTY_ARRAY;
      this.useParams = useParams;
    }

    JdbcParameterExpressionDeParser(Object[] parameters, List<Object> useParams) {
      ordinaryParameters = parameters;
      namedParameters = null;
      this.useParams = useParams;
    }

    @Override
    public void visit(JdbcNamedParameter jdbcParameter) {
      String name = jdbcParameter.getName();
      shouldBeTrue(namedParameters.containsKey(name),
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
    }

    @Override
    public void visit(JdbcParameter jdbcParameter) {
      if (jdbcParameter.isUseFixedIndex()) {
        super.visit(jdbcParameter);
        return;
      }
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
}

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
package org.corant.modules.query.sql;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.substring;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Objects;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午4:06:43
 *
 */
public class SqlStatements {

  public static Pair<String, Object[]> normalize(String sql, Map<String, Object> namedParameters) {
    try {
      StringBuilder statement = new StringBuilder();
      List<Object> useParams = new ArrayList<>();
      ExpressionDeParser expressionDeParser =
          new JdbcParameterExpressionDeParser(namedParameters, useParams);
      Select select = (Select) CCJSqlParserUtil.parse(sql);
      SelectDeParser deparser = new SelectDeParser(expressionDeParser, statement);
      expressionDeParser.setSelectVisitor(deparser);
      expressionDeParser.setBuffer(statement);
      select.getSelectBody().accept(deparser);
      return Pair.of(statement.toString(), useParams.toArray());
    } catch (JSQLParserException ex) {
      throw new QueryRuntimeException(ex);
    }
  }

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
      Select select = (Select) CCJSqlParserUtil.parse(sql);
      SelectDeParser deparser = new SelectDeParser(expressionDeParser, statement);
      expressionDeParser.setSelectVisitor(deparser);
      expressionDeParser.setBuffer(statement);
      select.getSelectBody().accept(deparser);
      return Pair.of(statement.toString(), useParams.toArray());
    } catch (JSQLParserException ex) {
      throw new QueryRuntimeException(ex);
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
        throw new QueryRuntimeException("SQL placeholder does not match the given parameter!");
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

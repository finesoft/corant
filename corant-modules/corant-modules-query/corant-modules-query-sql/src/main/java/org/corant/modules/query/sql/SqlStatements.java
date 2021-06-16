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
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
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

  public static Pair<String, Object[]> normalized(Map<String, Object> parameters, String sql) {
    StringBuilder statement = new StringBuilder();
    List<Object> useParams = new ArrayList<>();
    ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
      @Override
      public void visit(JdbcNamedParameter jdbcParameter) {
        String name = jdbcParameter.getName();
        shouldBeTrue(parameters.containsKey(name),
            "The named parameter [%s] in SQL does not match the given parameter!", name);
        Object parameter = parameters.get(name);
        List<Object> tempParams = new ArrayList<>();
        int size = 0;
        if (parameter instanceof Collection) {
          for (Object obj : (Collection<?>) parameter) {
            tempParams.add(obj);
            size++;
          }
        } else if (parameter != null && parameter.getClass().isArray()) {
          for (Object obj : (Object[]) parameter) {
            tempParams.add(obj);
            size++;
          }
        } else {
          tempParams.add(parameter);
          size++;
        }
        if (size == 1) {
          buffer.append("?");
        } else {
          for (int i = 0; i < size; i++) {
            buffer.append("?,");
          }
          buffer.deleteCharAt(buffer.length() - 1);
        }
        useParams.addAll(tempParams);
      }
    };
    try {
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

  public static Pair<String, Object[]> normalized(String sql, Object... parameters) {
    if (isBlank(sql) || parameters.length == 0
        || streamOf(parameters).noneMatch(p -> p instanceof Collection)) {
      return Pair.of(sql, parameters);
    }
    StringBuilder statement = new StringBuilder();
    List<Object> useParams = new ArrayList<>();
    ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
      @Override
      public void visit(JdbcParameter jdbcParameter) {
        if (jdbcParameter.isUseFixedIndex()) {
          super.visit(jdbcParameter);
          return;
        }
        if (jdbcParameter.getIndex() > parameters.length) {
          throw new QueryRuntimeException("SQL placeholder does not match the parameter!");
        }
        Object parameter = parameters[jdbcParameter.getIndex() - 1];
        List<Object> tempParams = new ArrayList<>();
        int size = 0;
        if (parameter instanceof Collection) {
          for (Object obj : (Collection<?>) parameter) {
            tempParams.add(obj);
            size++;
          }
        } else if (parameter != null && parameter.getClass().isArray()) {
          for (Object obj : (Object[]) parameter) {
            tempParams.add(obj);
            size++;
          }
        } else {
          tempParams.add(parameter);
          size++;
        }
        if (size == 1) {
          buffer.append("?");
        } else {
          for (int i = 0; i < size; i++) {
            buffer.append("?,");
          }
          buffer.deleteCharAt(buffer.length() - 1);
        }
        useParams.addAll(tempParams);
      }
    };
    try {
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
}

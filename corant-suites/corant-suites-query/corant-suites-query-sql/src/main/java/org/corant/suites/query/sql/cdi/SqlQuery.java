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
package org.corant.suites.query.sql.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import org.corant.shared.util.StringUtils;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;

/**
 * corant-suites-query-sql
 *
 * <P>
 * Sql name query service qualifier.
 *
 * <pre>
 *  The property value is data source name.
 *  The dialect is the data source dialect default is MySQL.
 * </pre>
 *
 * @author bingo 下午6:05:00
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Qualifier
public @interface SqlQuery {

  /**
   * The data source dialect
   *
   * @return dialect
   */
  @Nonbinding
  String dialect() default "";

  /**
   * The data source name
   *
   * @return value
   */
  @Nonbinding
  String value() default "";

  /**
   * corant-suites-query-sql
   *
   * @author bingo 上午11:42:04
   *
   */
  public static final class SqlQueryLiteral extends AnnotationLiteral<SqlQuery>
      implements SqlQuery {

    public static final SqlQuery INSTANCE = of(DBMS.MYSQL, "");

    private static final long serialVersionUID = 1L;

    private final String value;

    private final String dialect;

    private SqlQueryLiteral(String value, String dialect) {
      this.value = value;
      this.dialect = dialect;
    }

    public static SqlQueryLiteral of(DBMS dialect, String ds) {
      return new SqlQueryLiteral(ds, dialect == null ? StringUtils.EMPTY : dialect.name());
    }

    @Override
    public String dialect() {
      return dialect;
    }

    @Override
    public String value() {
      return value;
    }

  }
}

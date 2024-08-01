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
package org.corant.modules.query.sql.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Annotations.calculateMembersHashCode;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.datasource.shared.DBMS;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-sql
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
  String dialect() default EMPTY;

  /**
   * The data source name
   *
   * @return value
   */
  @Nonbinding
  String value() default EMPTY;

  /**
   * corant-modules-query-sql
   *
   * @author bingo 上午11:42:04
   *
   */
  final class SqlQueryLiteral extends AnnotationLiteral<SqlQuery> implements SqlQuery {

    public static final SqlQuery INSTANCE = of(DBMS.MYSQL, EMPTY);

    private static final long serialVersionUID = 1L;

    private String value;
    private String dialect;
    private transient volatile Integer hashCode;

    private SqlQueryLiteral(String value, String dialect) {
      this.value = defaultString(value);
      this.dialect = defaultString(dialect);
    }

    public static SqlQueryLiteral of(DBMS dialect, String ds) {
      return new SqlQueryLiteral(ds, dialect == null ? Qualifiers.EMPTY_NAME : dialect.name());
    }

    @Override
    public String dialect() {
      return dialect;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !SqlQuery.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      SqlQuery other = (SqlQuery) obj;
      return value.equals(other.value()) && dialect.equals(other.dialect());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("value", value), Pair.of("dialect", dialect));
      }
      return hashCode;
    }

    @Override
    public String value() {
      return value;
    }
  }
}

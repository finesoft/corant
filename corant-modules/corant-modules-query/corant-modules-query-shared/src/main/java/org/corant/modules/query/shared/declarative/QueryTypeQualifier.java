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
package org.corant.modules.query.shared.declarative;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.corant.modules.query.mapping.Query.QueryType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

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
public @interface QueryTypeQualifier {

  /**
   * The query qualifier, typically represents a data source, here data source means sql data source
   * or mongodb database or elastic and cassandra cluster.
   *
   * <pre>
   *
   *  1. In SQL query <i>({@code type()} is QueryType.SQL)</i>, the qualifier represents the
   *  data source name and the database dialect and use ':' to concat.
   *  example: blog:MYSQL, blog is the data source name, MYSQL is the dialect.
   *
   *  2. In Mongodb <i>(query {@code type()})</i> is QueryType.MG, the qualifier represents the
   *  mongodb data base name.
   *
   *  3. In elastic and cassandra query <i>({@code type()} is QueryType.ES or QueryType.CAS) </i>,
   *  the qualifier represents the cluster name.
   *
   *  Default is empty string, meaning that if there is only one data source for the particular
   *  query type ({@code type()}) in the application, then the qualifier represents that data source.
   *
   * </pre>
   *
   * @return qualifier
   */
  @Nonbinding
  String qualifier() default EMPTY;

  /**
   * Returns the Query type, used to indicate the type of query script supported by the query, and
   * also implies the type of data system.
   */
  @Nonbinding
  QueryType type() default QueryType.$$;

  /**
   * corant-modules-query-sql
   *
   * @author bingo 上午11:42:04
   *
   */
  final class QueryTypeQualifierLiteral extends AnnotationLiteral<QueryTypeQualifier>
      implements QueryTypeQualifier {

    private static final long serialVersionUID = -771777257818902465L;

    private final QueryType type;

    private final String qualifier;

    public QueryTypeQualifierLiteral(QueryType type, String qualifier) {
      this.type = type;
      this.qualifier = qualifier;
    }

    public static QueryTypeQualifierLiteral of(QueryType type, String qualifier) {
      return new QueryTypeQualifierLiteral(type, qualifier);
    }

    @Override
    public String qualifier() {
      return qualifier;
    }

    @Override
    public QueryType type() {
      return type;
    }

  }
}

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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.query.QueryService;
import org.corant.modules.query.mapping.Query.QueryType;

/**
 * corant-modules-query-shared
 *
 * The declarative query service interface use for a particular query service interface. An
 * interface specifying this annotation represents a particular query service, the query processor
 * will generate an application scope bean object with @AutoCreated qualifier implements the
 * interface.
 *
 * <pre>
 * NOTE:
 *
 * 1. The interface method with default static modifier will not be dynamic implemented.
 *
 * 2. If use Java8 then default method will available, this may be changed in the future.
 *
 * 3. For now if the method without default and static modifier and has more than one parameter,
 * the second parameter will not be use, we only use first parameter as query parameter,
 * this may be changed in the future.
 *
 * 4. The method return type must be same as the query result type.
 *
 * 5. Dynamically generated methods support facilities such as CDI interceptors
 *
 * 6. If the method is not annotated @QueryMethod, then the interface class simple name and
 * the method name are joined with a dot(.) as the name of the named query.
 * If the method name start with 'page' then implement as paging query {@link QueryService#page(Object, Object)};
 * If the method name start with 'forward' then implement as forwarding query {@link QueryService#forward(Object, Object)};
 * If the method name start with 'get' then implement as get query {@link QueryService#get(Object, Object)};
 * If the method name start with 'stream' then implement as stream query {@link QueryService#stream(Object, Object)};
 * If the method not start with above then implement as select query  {@link QueryService#select(Object, Object)}
 * </pre>
 *
 * @author bingo 下午6:30:18
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE})
@Inherited
@ApplicationScoped
@Stereotype
public @interface DeclarativeQueryService {

  /**
   * The query qualifier, typically represents a data source, here data source means sql data source
   * or mongodb database or elastic and cassandra cluster.
   *
   * <pre>
   *
   *  1. In SQL query <i>({@code
   * type()
   * } is QueryType.SQL)</i>, the qualifier represents the
   *  data source name and the database dialect and use ':' to concat.
   *  example: blog:MYSQL, blog is the data source name, MYSQL is the dialect.
   *
   *  2. In Mongodb <i>(query {@code
   * type()
   * })</i> is QueryType.MG, the qualifier represents the
   *  mongodb data base name.
   *
   *  3. In elastic and cassandra query <i>({@code
   * type()
   * } is QueryType.ES or QueryType.CAS) </i>,
   *  the qualifier represents the cluster name.
   *
   *  Default is empty string, meaning that if there is only one data source for the particular
   *  query type ({@code
   * type()
   * }) in the application, then the qualifier represents that data source.
   *
   * </pre>
   *
   * @return qualifier
   */
  String qualifier() default Qualifiers.EMPTY_NAME;

  /**
   * The query type
   *
   * @return type
   */
  QueryType type() default QueryType.SQL;

  /**
   * corant-modules-query-shared
   *
   * @author bingo 下午7:20:19
   *
   */
  class DeclarativeQueryServiceLiteral extends AnnotationLiteral<DeclarativeQueryService>
      implements DeclarativeQueryService {

    private static final long serialVersionUID = 8160691461505134491L;

    final String qualifier;
    final QueryType type;

    /**
     * @param type
     * @param qualifier
     */
    DeclarativeQueryServiceLiteral(QueryType type, String qualifier) {
      this.qualifier = qualifier;
      this.type = type;
    }

    public static DeclarativeQueryServiceLiteral of(QueryType type, String qualifier) {
      return new DeclarativeQueryServiceLiteral(type, qualifier);
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

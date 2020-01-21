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
package org.corant.suites.query.shared.declarative;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.suites.query.shared.mapping.Query.QueryType;

/**
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

  String qualifier();

  QueryType type() default QueryType.SQL;

  public static class DeclarativeQueryServiceLiteral
      extends AnnotationLiteral<DeclarativeQueryService> implements DeclarativeQueryService {

    private static final long serialVersionUID = 8160691461505134491L;

    final String qualifier;
    final QueryType type;

    /**
     * @param type
     * @param qualifier
     */
    DeclarativeQueryServiceLiteral(QueryType type, String qualifier) {
      super();
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

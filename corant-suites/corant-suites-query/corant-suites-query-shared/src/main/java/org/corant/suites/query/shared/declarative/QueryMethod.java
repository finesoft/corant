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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.corant.suites.query.shared.QueryService.QueryWay;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午2:41:19
 *
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
@Inherited
public @interface QueryMethod {

  /**
   * The name of query
   *
   * @return name
   */
  String name() default "";

  /**
   * The method that query service execute
   *
   * @return way
   */
  QueryWay way() default QueryWay.SELECT;
}

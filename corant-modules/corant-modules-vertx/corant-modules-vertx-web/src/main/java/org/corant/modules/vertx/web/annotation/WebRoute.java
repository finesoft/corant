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
package org.corant.modules.vertx.web.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * corant-modules-vertx-web
 *
 * @author bingo 下午2:27:36
 *
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface WebRoute {

  /**
   * @see Route#consumes(String)
   * @return consumes
   */
  String[] consumes() default {"application/json"};

  /**
   * @see Router#post()
   * @see Router#get()
   * @see Router#delete()
   * @see Router#patch()
   * @see Router#options()
   * @see Router#head()
   * @see Router#connect()
   * @see Router#trace()
   * @return methods
   */
  String[] methods() default {"GET", "POST"};

  /**
   * If set to {@link Integer#MIN_VALUE} the order of the route is not modified.
   */
  int order() default Integer.MIN_VALUE;

  /**
   * @see Route#produces(String)
   * @return consumes
   */
  String[] produces() default {"application/json"};

  /**
   * @see Router#regex(String)
   * @return value
   */
  String regex() default EMPTY;

  /**
   * @see Route#handler(Handler)
   * @see Route#blockingHandler(Handler)
   * @see Route#failureHandler(Handler)
   * @return type
   */
  HandlerType type() default HandlerType.NORMAL;

  /**
   * @see Router#route(String)
   * @return value
   */
  String value() default EMPTY;

  enum HandlerType {

    /**
     * A request handler.
     *
     * @see Route#handler(Handler)
     */
    NORMAL,
    /**
     * A blocking request handler.
     *
     * @see Route#blockingHandler(Handler)
     */
    BLOCKING,
    /**
     * A failure handler.
     *
     * @see Route#failureHandler(Handler)
     */
    FAILURE

  }

}

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
package org.corant.context.concurrent.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import org.corant.context.qualifier.Qualifiers;

/**
 * corant-context
 *
 * @author bingo 上午9:49:43
 *
 */
@InterceptorBinding
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Asynchronous {

  @Nonbinding
  String[] abortOn() default {};

  @Nonbinding
  String backoffFactor() default "2.0";

  @Nonbinding
  String backoffStrategy() default "FIXED";

  @Nonbinding
  String baseBackoffDuration() default "PT0S";

  @Nonbinding
  String executor() default Qualifiers.EMPTY_NAME;

  @Nonbinding
  String maxAttempts() default EMPTY;

  @Nonbinding
  String maxBackoffDuration() default "PT0S";

  @Nonbinding
  String[] retryOn() default {};

  @Nonbinding
  String timeout() default EMPTY;
}

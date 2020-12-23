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
package org.corant.suites.quartz;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;

/**
 * corant-suites-quartz
 *
 * @author bingo 上午10:19:58
 *
 */
@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface Scheduled {

  String cronExpression();

  String description() default "";

  Class<?> group() default Scheduled.class;

  boolean onStartup() default true;

  boolean overrideOnStartup() default false;

  Class<? extends Annotation>[] startScopes() default {SessionScoped.class, RequestScoped.class};
}

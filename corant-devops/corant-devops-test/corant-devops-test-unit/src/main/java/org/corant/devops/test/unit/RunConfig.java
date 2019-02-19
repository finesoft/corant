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
package org.corant.devops.test.unit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.corant.shared.normal.Priorities.ConfigPriorities;

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午7:09:58
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface RunConfig {

  /**
   * additional config property, the priorities is tmp
   *
   * @see ConfigPriorities.APPLICATION_TMP_ORDINAL
   * @return properties
   */
  AddiConfigProperty[] addiConfigProperties() default {};

  /**
   * Dispose Corant instance, clear test object and resource after test end in test thread. If use
   * in test suite, the children of suite not affected. all dispose operation will be run after test
   * suite finished when test suite class set autoDispose true.
   *
   * @return autoDispose
   */
  boolean autoDispose() default true;

  /**
   * The config class
   *
   * @return configClass
   */
  Class<?> configClass();

  /**
   * Use random web port for test, usually use with web server environment testing. If use in test
   * suite, the children of suite not affected.
   *
   * @return randomWebPort
   */
  boolean randomWebPort() default false;

  /**
   * Assign particularly profile configuration for test environment. If use in test suite, the
   * children of suite not affected.
   *
   * @return profile
   */
  String profile() default "";
}

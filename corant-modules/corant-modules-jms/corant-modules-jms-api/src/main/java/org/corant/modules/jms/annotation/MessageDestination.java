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
package org.corant.modules.jms.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午2:13:25
 *
 */
@Retention(RUNTIME)
@Target({TYPE, FIELD, METHOD, PARAMETER})
@Inherited
public @interface MessageDestination {

  /**
   * The connection factory id, used to represent a JMS service or cluster, usually set up through a
   * configuration file, if the value uses the <b>"${...}"</b> expression, the specific value can be
   * obtained from the system property or configuration.
   *
   * Default is empty that means unspecified. At the same time the connection factory id is used for
   * CDI qualifier.
   */
  String connectionFactoryId() default EMPTY;

  /**
   * Marks whether a queue or topic.
   * <p>
   * Value of true represents Topic, Value of false represents Queue, default is false.
   * <p>
   * <b>Note:</b> The final value type is <b>boolean</b> type; in order to support configurability,
   * the string is used as the value type of annotation property, and the value will eventually be
   * converted to boolean type. If the value of this property uses the <b>"${...}"</b> expression,
   * the specific value can be obtained from the system property or configuration, and then convert
   * it to boolean value.
   *
   * @return whether is Topic
   */
  String multicast() default "false";

  /**
   * The destination name
   * <p>
   * Note: If the value of this property uses the <b>"${...}"</b> expression, the specific value can
   * be obtained from the system property or configuration.
   */
  String name();

  /**
   * Jakarta Messaging destination property. This may be a vendor-specific property or a less
   * commonly used {@code ConnectionFactory} property.
   *
   * <p>
   * Properties are specified using the format: <i>propertyName=propertyValue</i> with one property
   * per array element.
   *
   * @return The Jakarta Messaging destination properties.
   */
  MessageProperty[] properties() default {};

}

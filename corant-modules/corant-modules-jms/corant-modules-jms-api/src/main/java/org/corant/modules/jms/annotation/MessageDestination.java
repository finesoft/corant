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
public @interface MessageDestination {

  /**
   * The connection factory id, used to represent a JMS service or cluster, usually set up through a
   * configuration file, can use '${config property name}' to retrieve the connection factory id
   * from microprofile config source.
   */
  String connectionFactoryId() default EMPTY;

  /**
   * Marks whether a queue or topic.
   *
   * Value of true represents Topic, Value of false represents Queue, default is false.
   *
   * @return multicast
   */
  boolean multicast() default false;

  /**
   * The destination name, can use '${config property name}' to retrieve the destination name from
   * microprofile config source.
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
  String[] properties() default {};

}

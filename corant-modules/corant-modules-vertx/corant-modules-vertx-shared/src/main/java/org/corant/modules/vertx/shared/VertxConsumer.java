/*
 * JBoss, Home of Professional Open Source Copyright 2016, Red Hat, Inc., and individual
 * contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.corant.modules.vertx.shared;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * An observer method must declare an event parameter of the type {@link VertxEvent} with
 * {@link VertxConsumer} qualifier in order to be notified when a message is sent via
 * {@link io.vertx.core.eventbus.EventBus}.
 *
 * @author Martin Kouba
 * @see VertxEvent
 */
@Qualifier
@Target({TYPE, METHOD, PARAMETER, FIELD})
@Retention(RUNTIME)
public @interface VertxConsumer {

  /**
   * @return the address the consumer will be registered to
   */
  String value();

  final class Literal extends AnnotationLiteral<VertxConsumer> implements VertxConsumer {

    private static final long serialVersionUID = 1L;

    private final String value;

    private Literal(String value) {
      this.value = value;
    }

    public static Literal of(String value) {
      return new Literal(value);
    }

    @Override
    public String value() {
      return value;
    }

  }

}

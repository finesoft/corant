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
package org.corant.modules.vertx.web.command;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.context.command.Commands;
import org.corant.modules.vertx.web.annotation.WebRoute;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;

/**
 * corant-modules-vertx-web
 *
 * @author bingo 下午2:48:09
 *
 */
public class VertxCommandExtension implements Extension {

  final Map<Class<?>, Set<Class<? extends VertxCommandHandler<?>>>> commandAndHandler =
      new ConcurrentHashMap<>();

  public Set<Class<? extends VertxCommandHandler<?>>> getCommandHandlerTypes(
      Class<?> commandClass) {
    return commandAndHandler.get(commandClass);
  }

  public Set<Class<?>> getCommands() {
    return new HashSet<>(commandAndHandler.keySet());
  }

  protected void arrange(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) @WithAnnotations({
      Commands.class}) ProcessAnnotatedType<?> event) {

  }

  protected Set<WebRoute> resolveWebRoute(Set<Class<?>> commandClasses) {
    Set<WebRoute> wrs = new LinkedHashSet<>();
    for (Class<?> clazz : commandClasses) {
      if (clazz.isAnnotationPresent(WebRoute.class)
          && VertxCommandHandler.class.isAssignableFrom(clazz)
          && !wrs.add(clazz.getAnnotation(WebRoute.class))) {
        throw new CorantRuntimeException("The route dup");
      }
    }
    return wrs;
  }

}

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
package org.corant.context.command;

import static org.corant.config.Configs.getValue;
import static org.corant.shared.util.Sets.newHashSet;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.annotation.Priority;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Services;

/**
 * corant-context
 *
 * @author bingo 下午10:08:26
 */
public class CommandExtension implements Extension {

  private static final Logger logger = Logger.getLogger(CommandExtension.class.getName());

  static final boolean ENABLED_COMMANDS =
      getValue("corant.context.command.enable", Boolean.class, Boolean.TRUE);

  static final boolean SUPPORT_ABSTRACT_COMMAND =
      getValue("corant.context.command.support-abstract-command", Boolean.class, Boolean.FALSE);

  final Map<Class<?>, Set<Class<? extends CommandHandler<?>>>> commandAndHandler =
      new ConcurrentHashMap<>();

  public Set<Class<? extends CommandHandler<?>>> getCommandHandlerTypes(Class<?> commandClass) {
    return commandAndHandler.get(commandClass);
  }

  public Set<Class<?>> getCommands() {
    return new HashSet<>(commandAndHandler.keySet());
  }

  @SuppressWarnings("unchecked")
  void arrange(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) @WithAnnotations({
      Commands.class}) ProcessAnnotatedType<?> event) {
    if (ENABLED_COMMANDS) {
      Class<?> handlerCls = event.getAnnotatedType().getJavaClass();
      if (!handlerCls.isInterface() && !Modifier.isAbstract(handlerCls.getModifiers())
          && CommandHandler.class.isAssignableFrom(handlerCls)) {
        Class<?> cmdCls = resolveCommandType(handlerCls);
        if (cmdCls == null) {
          logger.warning(() -> String
              .format("Can not find any command type parameter for handler [%s]", handlerCls));
        } else if (!Services.shouldVeto(handlerCls)) {
          if (!cmdCls.isInterface() && !Modifier.isAbstract(cmdCls.getModifiers())) {
            commandAndHandler.computeIfAbsent(cmdCls, k -> new HashSet<>())
                .add((Class<? extends CommandHandler<?>>) handlerCls);
            logger.fine(() -> String.format("Resolved the command [%s] with handler [%s]", cmdCls,
                handlerCls));
          } else if (SUPPORT_ABSTRACT_COMMAND) {
            commandAndHandler.computeIfAbsent(cmdCls, k -> new HashSet<>())
                .add((Class<? extends CommandHandler<?>>) handlerCls);
            logger.fine(() -> String.format("Resolved the abstract command [%s] with handler [%s]",
                cmdCls, handlerCls));
          } else {
            logger.warning(() -> String.format(
                "The command class [%s] extract from handler [%s] must be a concrete class", cmdCls,
                handlerCls));
          }
        }
      }
    }
  }

  synchronized void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    commandAndHandler.clear();
    logger.fine(() -> "Clear command & handlers cache.");
  }

  synchronized void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    List<Pair<Class<?>, Set<Class<? extends CommandHandler<?>>>>> warnings = new ArrayList<>();
    commandAndHandler.forEach((k, v) -> {
      if (v.size() > 1) {
        warnings.add(Pair.of(k, v));
      }
    });
    if (warnings.size() > 0) {
      StringBuilder errMsg = new StringBuilder("The command & handler mismatching:");
      warnings.forEach(e -> errMsg.append("\n  ").append(e.key().getName()).append(" -> ")
          .append(e.value().stream().map(Class::getName).collect(Collectors.joining(",")))
          .append(";"));
      warnings.clear();
      logger.warning(errMsg::toString);
      // TODO FIXME since we are using CDI, so the command handler bean may have qualifiers
      // adv.addDeploymentProblem(new CorantRuntimeException(errMsg.toString()));
    }
    if (!commandAndHandler.isEmpty()) {
      logger.fine(() -> String.format("Found %s command handlers", commandAndHandler.size()));
    }
    // Make immutable
    for (Class<?> cls : newHashSet(commandAndHandler.keySet())) {
      commandAndHandler.put(cls, Collections.unmodifiableSet(commandAndHandler.get(cls)));
    }
  }

  private Class<?> resolveCommandType(Class<?> refCls) {
    Class<?> resolvedClass = null;
    Class<?> clazz = refCls;
    do {
      if (clazz.getGenericSuperclass() instanceof ParameterizedType) {
        resolvedClass = (Class<?>) ((ParameterizedType) clazz.getGenericSuperclass())
            .getActualTypeArguments()[0];
        break;
      } else {
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type type : genericInterfaces) {
          if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() instanceof Class) {
              resolvedClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
              break;
            }
          }
        }
      }
    } while (resolvedClass == null && (clazz = clazz.getSuperclass()) != null);
    return resolvedClass;
  }
}

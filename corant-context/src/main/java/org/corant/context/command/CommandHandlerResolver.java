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

import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * corant-context
 *
 * <p>
 * The command handler resolver is used to resolve the appropriate command handler with the given
 * command and handler qualifiers
 * <p>
 * Note: Users can use CDI {@link Specializes} to replace and inherit this class by themselves, and
 * re-implement the {@link #resolve(Object, Annotation...)} method to enable it to face more complex
 * application scenarios, for example, it can support the inheritance relationship of command object
 * types, etc.
 *
 * @see CommandExtension#arrange(javax.enterprise.inject.spi.ProcessAnnotatedType)
 *
 * @author bingo 下午10:01:33
 *
 */
@Singleton
public class CommandHandlerResolver {

  @Inject
  protected BeanManager beanManager;

  @Inject
  protected CommandExtension extension;

  @Inject
  @Any
  protected Instance<CommandValidator> validators;

  /**
   * Returns the appropriate command handler with the given command and handler qualifiers.
   *
   * @param <C> the commands type
   * @param cmd the commands object
   * @param qualifiers the command handler bean additional qualifiers
   */
  public <C> CommandHandler<C> resolve(C cmd, Annotation... qualifiers) {
    if (cmd != null) {
      if (validators.isResolvable()) {
        validators.get().validate(cmd);
      }
      Set<Class<? extends CommandHandler<?>>> handlerClasses =
          extension.getCommandHandlerTypes(cmd.getClass());
      CommandHandler<C> handler = null;
      int size = sizeOf(handlerClasses);
      if (size == 1) {
        handler = resolve(handlerClasses.iterator().next(), qualifiers);
      } else if (size > 1) {
        for (Class<? extends CommandHandler<?>> handlerClass : handlerClasses) {
          CommandHandler<C> resolvedHandler = resolve(handlerClass, qualifiers);
          if (resolvedHandler != null) {
            if (handler == null) {
              handler = resolvedHandler;
            } else if (!getUserClass(resolvedHandler.getClass())
                .equals(getUserClass(handler.getClass()))) {
              // Filter @Specializes
              throw new AmbiguousResolutionException("Can't resolve command handler for "
                  + cmd.getClass() + " and ambiguous handlers " + String.join(",",
                      handlerClasses.stream().map(Class::getName).toArray(String[]::new)));
            }
          }
        }
      }
      if (handler != null) {
        return handler;
      }
    }
    throw new UnsatisfiedResolutionException("Can't resolve command handler for " + cmd.getClass());
  }

  @SuppressWarnings("unchecked")
  protected <C> CommandHandler<C> resolve(Class<? extends CommandHandler<?>> handlerClass,
      Annotation... qualifiers) {
    CommandHandler<C> handler = null;
    Set<Bean<?>> beans = beanManager.getBeans(handlerClass, qualifiers);
    if (isNotEmpty(beans)) {
      if (beans.size() > 1) {
        beans =
            beans.stream().filter(b -> (b.getBeanClass().equals(handlerClass) || b.isAlternative()))
                .collect(Collectors.toSet());
      }
      if (isNotEmpty(beans)) {
        Bean<?> bean = beanManager.resolve(beans);
        if (bean != null) {
          CreationalContext<?> context = beanManager.createCreationalContext(bean);
          handler =
              context != null
                  ? (CommandHandler<C>) handlerClass
                      .cast(beanManager.getReference(bean, handlerClass, context))
                  : null;
        }
      }
    }
    return handler;
  }

}

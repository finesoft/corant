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

import static org.corant.shared.util.Lists.appendIfAbsent;
import java.lang.annotation.Annotation;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.corant.context.qualifier.TypeArgument.TypeArgumentLiteral;

/**
 * corant-context
 *
 * <p>
 * The command handler resolver is use for resolve the appropriate command handler with the given
 * command and handler qualifiers.
 * <p>
 * Note: User can use CDI {@link Specializes} to replace and inherit this class by themselves, and
 * re-implement the {@link #resolve(Object, Annotation...)} method to enable it to face more complex
 * application scenarios, for example, it can support the inheritance relationship of command object
 * types, etc.
 *
 * @see CommanderExtension#arrange(javax.enterprise.inject.spi.ProcessAnnotatedType)
 *
 * @author bingo 下午10:01:33
 *
 */
@Singleton
public class CommandHandlerResolver {

  @Inject
  @Any
  protected Instance<CommandHandler<?>> handlers;

  /**
   * Returns the appropriate command hander with the given command and handler qualifiers.
   *
   * @param <C> the commands type
   * @param cmd the commands object
   * @param qualifiers the command handler bean additional qualifiers
   */
  @SuppressWarnings("unchecked")
  public <C> CommandHandler<C> resolve(C cmd, Annotation... qualifiers) {
    if (handlers.isUnsatisfied()) {
      throw new UnsatisfiedResolutionException(
          "Can't resolve command handler for " + cmd.getClass());
    }
    return (CommandHandler<C>) handlers
        .select(appendIfAbsent(qualifiers, TypeArgumentLiteral.of(cmd.getClass()))).get();
  }

  @SuppressWarnings({"unchecked", "serial"})
  protected <C> CommandHandler<C> simpleRresolve(C cmd, Annotation... qualifiers) {
    return (CommandHandler<C>) resolve(new TypeLiteral<CommandHandler<?>>() {},
        appendIfAbsent(qualifiers, TypeArgumentLiteral.of(cmd.getClass())));
  }
}

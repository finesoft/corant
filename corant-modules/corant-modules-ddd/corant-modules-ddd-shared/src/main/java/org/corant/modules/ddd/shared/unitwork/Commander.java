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
package org.corant.modules.ddd.shared.unitwork;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.appendIfAbsent;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import java.lang.annotation.Annotation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.util.TypeLiteral;
import org.corant.modules.ddd.CommandHandler;
import org.corant.modules.ddd.Commands;
import org.corant.modules.ddd.shared.annotation.CMDS.CMDSLiteral;

/**
 * corant-modules-ddd-shared
 * <p>
 * The class is the realization of a simple command bus. This class method is used to automatically
 * invoke the command handler synchronously or asynchronously according to the given command object
 * type. To use this, you need a commands that implements the
 * {@link org.corant.modules.ddd.Commands} interface and one or more command handlers that use this
 * commands type as generic parameter type and inherit the abstract class
 * {@link org.corant.modules.ddd.shared.unitwork.AbstractCommandHandler}.
 * <p>
 * Note: If a command has multiple handlers, follow CDI Type Safe Resolution calls one of the
 * handlers that meet the context requirements for handling, and throws an exception when the
 * handler that meets the context requirements cannot be resolved, for example, more than one is
 * found or not found.
 *
 * <p>
 * <b>The commands:</b>
 *
 * <pre>
 * public class MyCommand implements Commands {
 *   String data;
 *
 *   public MyCommand(String data) {
 *     this.data = data;
 *   }
 *
 *   public String getData() {
 *     return data;
 *   }
 * }
 * </pre>
 *
 * <b>The commands handler:</b>
 *
 * <pre>
 * public class MyCommandHandler extends AbstractCommandHandler&lt;MyCommand&gt; {
 *
 *   public String handle(MyCommand command) {
 *     return command.getData().toUpperCase(Locale.ROOT);
 *   }
 * }
 * </pre>
 *
 * <b>The commander use case:</b>
 * <ul>
 * <li>accept: Commander.accept(new MyCommand("test"));</li>
 * <li>apply: String upperCase = Commander.apply(new MyCommand("test"));</li>
 * <li>asynchronous accept: Commander.async().accept(new MyCommand("test"));</li>
 * <li>asynchronous apply: Future&lt;String&gt; upperCase = Commander.async().apply(new
 * MyCommand("test"));</li>
 * <li>scheduled accept: Commander.schedule(10L,TimeUnit.SECONDS).accept(new
 * MyCommand("test"));</li>
 * <li>scheduled apply: ScheduledFuture&lt;String&gt; upperCase =
 * Commander.async(10L,TimeUnit.SECONDS).apply(new MyCommand("test"));</li>
 * </ul>
 *
 * @see CommandHandler
 * @see AbstractCommandHandler
 * @see Commands
 * @author bingo 下午9:05:28
 *
 */
public class Commander {

  /**
   * Accept and execute a given commands
   *
   * @param <C> the commands type
   * @param cmd the commands object
   * @param qualifiers the command handler bean additional qualifiers
   */
  public static <C extends Commands> void accept(C cmd, Annotation... qualifiers) {
    @SuppressWarnings({"unchecked", "serial"})
    CommandHandler<C> handler = (CommandHandler<C>) resolve(new TypeLiteral<CommandHandler<?>>() {},
        appendIfAbsent(qualifiers, CMDSLiteral.of(cmd.getClass())));
    handler.handle(cmd);
  }

  /**
   * Apply and execute a command and return the command execution result
   *
   * @param <C> the commands type
   * @param cmd the commands object
   * @param qualifiers the command handler bean additional qualifiers
   */
  @SuppressWarnings("unchecked")
  public static <R, C extends Commands> R apply(C cmd, Annotation... qualifiers) {
    @SuppressWarnings({"serial"})
    CommandHandler<C> handler = (CommandHandler<C>) resolve(new TypeLiteral<CommandHandler<?>>() {},
        appendIfAbsent(qualifiers, CMDSLiteral.of(cmd.getClass())));
    return (R) shouldNotNull(handler).handle(cmd);
  }

  /**
   * Returns an AsyncCommandExecutor that use managed executor service to accept or apply a
   * commands.
   *
   * @param executorQualifiers the managed executor service additional qualifiers
   */
  public static AsyncCommandExecutor async(Annotation... executorQualifiers) {
    return async(resolve(ManagedExecutorService.class, executorQualifiers));
  }

  /**
   * Returns an AsyncCommandExecutor that use the given executor service to accept or apply a
   * commands.
   *
   * @param executorService the executor service use to accept or apply a commands.
   */
  public static AsyncCommandExecutor async(ExecutorService executorService) {
    return new AsyncCommandExecutor(executorService);
  }

  /**
   * Returns an AsyncCommandExecutor that use managed executor service to accept or apply a
   * commands.
   *
   * @param delay the commands execution delay time
   * @param unit the commands execution delay time unit
   * @param executorQualifiers the managed scheduled executor service additional qualifiers
   */
  public static ScheduledCommandExecutor schedule(long delay, TimeUnit unit,
      Annotation... executorQualifiers) {
    return schedule(resolve(ManagedScheduledExecutorService.class, executorQualifiers), delay,
        unit);
  }

  /**
   * Returns an AsyncCommandExecutor that use the given scheduled executor service to accept or
   * apply a commands.
   *
   * @param scheduledExecutorService the scheduled executor service use to accept or apply a
   *        commands.
   * @param delay the commands execution delay time
   * @param unit the commands execution delay time unit
   */
  public static ScheduledCommandExecutor schedule(ScheduledExecutorService scheduledExecutorService,
      long delay, TimeUnit unit) {
    return new ScheduledCommandExecutor(scheduledExecutorService, delay, unit);
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午2:03:36
   *
   */
  public static class AsyncCommandExecutor {

    protected final ExecutorService es;

    protected AsyncCommandExecutor(ExecutorService es) {
      this.es = es;
    }

    /**
     * Accept and execute a given commands
     *
     * @param <C> the commands type
     * @param cmd the commands object
     * @param qualifiers the command handler bean additional qualifiers
     */
    public <C extends Commands> void accept(C cmd, Annotation... qualifiers) {
      es.execute(() -> Commander.accept(cmd, qualifiers));
    }

    /**
     * Apply and execute a command and return the command execution future result
     *
     * @param <C> the commands type
     * @param cmd the commands object
     * @param qualifiers the command handler bean additional qualifiers
     */
    public <R, C extends Commands> Future<R> apply(C cmd, Annotation... qualifiers) {
      return es.submit(() -> Commander.apply(cmd, qualifiers));
    }
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午2:03:40
   *
   */
  public static class ScheduledCommandExecutor {

    protected final ScheduledExecutorService es;
    protected final long delay;
    protected final TimeUnit unit;

    protected ScheduledCommandExecutor(ScheduledExecutorService es, long delay, TimeUnit unit) {
      this.es = es;
      this.delay = max(0L, delay);
      this.unit = defaultObject(unit, TimeUnit.NANOSECONDS);
    }

    /**
     * Accept and execute a given commands
     *
     * @param <C> the commands type
     * @param cmd the commands object
     * @param qualifiers the command handler bean additional qualifiers
     */
    public <C extends Commands> void accept(C cmd, Annotation... qualifiers) {
      es.schedule(() -> Commander.accept(cmd, qualifiers), delay, unit);
    }

    /**
     * Apply and execute a command and return the command execution scheduled future result
     *
     * @param <C> the commands type
     * @param cmd the commands object
     * @param qualifiers the command handler bean additional qualifiers
     */
    public <R, C extends Commands> ScheduledFuture<R> apply(C cmd, Annotation... qualifiers) {
      return es.schedule(() -> Commander.apply(cmd, qualifiers), delay, unit);
    }
  }
}

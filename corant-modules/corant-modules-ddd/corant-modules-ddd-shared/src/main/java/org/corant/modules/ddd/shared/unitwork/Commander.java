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
import static org.corant.shared.util.Lists.append;
import java.lang.annotation.Annotation;
import java.util.function.Consumer;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.util.TypeLiteral;
import org.corant.modules.ddd.shared.annotation.CMDS.CMDSLiteral;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午9:05:28
 *
 */
public class Commander {

  public static <C extends Commands> void accept(C cmd, Annotation... qualifiers) {
    @SuppressWarnings({"unchecked", "serial"})
    CommandHandler<C> handler = (CommandHandler<C>) resolve(new TypeLiteral<CommandHandler<?>>() {},
        append(qualifiers, CMDSLiteral.of(cmd.getClass())));
    handler.handle(cmd);
  }

  public static <C extends Commands> Consumer<Annotation[]> acceptAsync(C cmd,
      Annotation... qualifiers) {
    return t -> resolve(ManagedExecutorService.class, t)
        .execute(() -> Commander.accept(cmd, qualifiers));
  }

  @SuppressWarnings("unchecked")
  public static <R, C extends Commands> R apply(C cmd, Annotation... qualifiers) {
    @SuppressWarnings({"serial"})
    CommandHandler<C> handler = (CommandHandler<C>) resolve(new TypeLiteral<CommandHandler<?>>() {},
        append(qualifiers, CMDSLiteral.of(cmd.getClass())));
    return (R) shouldNotNull(handler).handle(cmd);
  }

}

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

import org.corant.context.command.Commands;

/**
 * corant-modules-vertx-web
 * <p>
 * Generic vert.x command handler abstract class, subclasses can inherit this abstract class to
 * implement command handling. If skip this type and directly implement the
 * {@link org.corant.modules.vertx.web.command.VertxCommandHandler} interface, you need to mark the
 * {@link org.corant.context.command.Commands} annotation on the implementation type so that it can
 * be automatically processed when the CDI container starts.
 *
 * @author bingo 下午1:13:52
 *
 */
@Commands
public abstract class AbstractVertxCommandHandler<C> implements VertxCommandHandler<C> {

}

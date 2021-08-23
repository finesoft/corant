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

import org.corant.modules.ddd.CommandHandler;
import org.corant.modules.ddd.Commands;
import org.corant.modules.ddd.annotation.CommandHandlers;

/**
 * corant-modules-ddd-shared
 *
 * Generic command handler abstract class, subclasses can inherit this abstract class to implement
 * command handling. If skip this type and directly implement the
 * {@link org.corant.modules.ddd.CommandHandler} interface, you need to mark the
 * {@link org.corant.modules.ddd.annotation.CommandHandlers} annotation on the implementation type
 * so that it can be automatically processed when the CDI container starts.
 *
 * @author bingo 下午1:13:52
 *
 */
@CommandHandlers
public abstract class AbstractCommandHandler<C extends Commands> implements CommandHandler<C> {

}

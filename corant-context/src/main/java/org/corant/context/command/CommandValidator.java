/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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

/**
 * corant-context
 *
 * <p>
 * The command validator interface used to verify whether the command object is valid, only valid
 * commands can be processed by the command handler. The implementation of the processor can use
 * {@code JSR 303}.
 *
 * @author bingo 下午9:54:06
 *
 */
public interface CommandValidator {

  /**
   * Validate the given command, if the given command is invalid or illegal the method must throw a
   * runtime exception.
   *
   * @param command the command to be validated
   */
  void validate(Object command);
}

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
package org.corant.kernel.util;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Lists.removeIf;
import static org.corant.shared.util.Strings.escapedSplit;
import static org.corant.shared.util.Strings.replace;
import static org.corant.shared.util.Strings.split;
import java.util.Arrays;
import org.corant.shared.util.Iterables;
import org.corant.shared.util.Strings;

/**
 * corant-kernel
 *
 * @author bingo 下午2:34:19
 */
public class CommandLine {

  final static String ESCAPED = Strings.BACK_SLASH + Strings.COMMA;

  final String command;
  final String[] arguments;

  public CommandLine(String command) {
    this.command = command;
    arguments = Strings.EMPTY_ARRAY;
  }

  public CommandLine(String command, String... arguments) {
    this.command = command;
    this.arguments = arguments;
  }

  public static CommandLine parse(String command, String... commandAndArguments) {
    if (commandAndArguments != null) {
      String cmdAndArgs = null;
      for (String arg : commandAndArguments) {
        if (arg != null && arg.startsWith(command)) {
          cmdAndArgs = arg;
          break;
        }
      }
      if (cmdAndArgs != null) {
        String[] temp = split(cmdAndArgs, Strings.EQUALS, true, true);
        if (temp.length > 1) {
          return new CommandLine(temp[0],
              removeIf(escapedSplit(temp[1], Strings.BACK_SLASH, Strings.COMMA), String::isEmpty));
        } else {
          return new CommandLine(temp[0]);
        }
      }
    }
    return null;
  }

  public String[] getArguments() {
    return hasArguments() ? Arrays.copyOf(arguments, arguments.length) : Strings.EMPTY_ARRAY;
  }

  public String getCommand() {
    return command;
  }

  public boolean hasArguments() {
    return arguments.length > 0;
  }

  public boolean hasArguments(String argument) {
    return argument != null && Iterables.search(arguments, argument) != -1;
  }

  @Override
  public String toString() {
    return arguments.length > 0
        ? command.concat(Strings.EQUALS).concat(String.join(Strings.COMMA, listOf(arguments)
            .stream().map(a -> replace(a, Strings.COMMA, ESCAPED)).toArray(String[]::new)))
        : command;
  }

}

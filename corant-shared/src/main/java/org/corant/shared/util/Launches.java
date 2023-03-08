/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.util;

import static java.util.Collections.emptyList;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.SPACE;
import static org.corant.shared.util.Strings.split;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;

/**
 * corant-shared
 *
 * @author bingo 上午10:37:10
 *
 */
public class Launches {

  private static final Pattern debugPattern = Pattern.compile("-Xdebug|jdwp");

  private Launches() {}

  public static void debugForkAs() throws IOException, InterruptedException {
    List<String> commands = new ArrayList<>();
    commands.add(getJavaHome());
    commands.addAll(getJvmArgs());
    commands.add(getDebugArgs());
    commands.addAll(getClassPathArgs(null));
    commands.addAll(getMainCommands());
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(commands);
    builder.inheritIO();
    Process process = builder.start();
    process.waitFor();
  }

  public static void forkAs(UnaryOperator<String> commandAdjuster)
      throws IOException, InterruptedException {
    UnaryOperator<String> used = defaultObject(commandAdjuster, UnaryOperator.identity());
    List<String> commands =
        getCommands().stream().map(used).filter(Objects::isNotNull).collect(Collectors.toList());
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(commands);
    builder.inheritIO();
    Process process = builder.start();
    process.waitFor();
  }

  public static List<String> getClassPathArgs(Predicate<String> filter) {
    List<String> args = new ArrayList<>();
    args.add("-classpath");
    args.add(getClassPaths().stream().filter(cp -> filter == null || filter.test(cp))
        .collect(Collectors.joining(File.pathSeparator)));
    return args;
  }

  public static List<String> getClassPaths() {
    return Arrays.stream(Systems.getClasspath()).filter(Strings::isNotBlank)
        .collect(Collectors.toList());
  }

  public static List<String> getCommands() {
    List<String> commands = new ArrayList<>();
    commands.add(getJavaHome());
    commands.addAll(getJvmArgs());
    commands.addAll(getClassPathArgs(null));
    commands.addAll(getMainCommands());
    return commands;
  }

  public static String getDebugArgs() {
    return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000";
  }

  public static Long getFreeMemoryMb() {
    return Runtime.getRuntime().freeMemory() / Defaults.ONE_MB;
  }

  public static String getJavaHome() {
    return Paths.get(Systems.getJavaHomeDir()).resolve("bin").resolve("java").toString();
  }

  public static String getJavaVersion() {
    return Systems.getJavaVersion();
  }

  public static List<String> getJvmArgs() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments();
  }

  public static List<String> getMainCommands() {
    return Arrays.stream(split(Systems.getProperty("sun.java.command"), SPACE))
        .filter(Strings::isNotBlank).collect(Collectors.toList());
  }

  public static Long getMaxMemoryMb() {
    return Runtime.getRuntime().maxMemory() / Defaults.ONE_MB;
  }

  public static String getPid() {
    try {
      String jvmName = ManagementFactory.getRuntimeMXBean().getName();
      return jvmName.split("@")[0];
    } catch (Throwable ex) {
      return null;
    }
  }

  public static Long getTotalMemoryMb() {
    return Runtime.getRuntime().totalMemory() / Defaults.ONE_MB;
  }

  public static Long getUsedMemoryMb() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
        / Defaults.ONE_MB;
  }

  public static boolean isDebugging() {
    for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (debugPattern.matcher(arg).find()) {
        return true;
      }
    }
    return false;
  }

  public static Launcher launcher() {
    return new Launcher();
  }

  public static void restartAs(UnaryOperator<String> commandAdjuster) throws IOException {
    UnaryOperator<String> used = defaultObject(commandAdjuster, UnaryOperator.identity());
    List<String> commands =
        getCommands().stream().map(used).filter(Objects::isNotNull).collect(Collectors.toList());
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(commands);
    builder.start();
    System.exit(0);
  }

  public static class Launcher {
    LaunchMode mode = LaunchMode.NORMAL;
    UnaryOperator<String> javaHomeReviser = UnaryOperator.identity();
    UnaryOperator<List<String>> jvmArgsReviser = UnaryOperator.identity();
    UnaryOperator<List<String>> classPathArgsReviser = UnaryOperator.identity();
    UnaryOperator<List<String>> mainCommandsReviser = UnaryOperator.identity();
    UnaryOperator<String> debugArgsReviser = UnaryOperator.identity();

    public Launcher classPathArgsReviser(final UnaryOperator<List<String>> classPathArgsReviser) {
      if (classPathArgsReviser != null) {
        this.classPathArgsReviser = classPathArgsReviser;
      }
      return this;
    }

    public Launcher debugArgsReviser(final UnaryOperator<String> debugArgsReviser) {
      if (javaHomeReviser != null) {
        this.debugArgsReviser = debugArgsReviser;
      }
      return this;
    }

    public Launcher javaHomeReviser(final UnaryOperator<String> javaHomeReviser) {
      if (javaHomeReviser != null) {
        this.javaHomeReviser = javaHomeReviser;
      }
      return this;
    }

    public Launcher jvmArgsReviser(final UnaryOperator<List<String>> jvmArgsReviser) {
      if (jvmArgsReviser != null) {
        this.jvmArgsReviser = jvmArgsReviser;
      }
      return this;
    }

    public Process launch() throws IOException {
      List<String> commands = new ArrayList<>();
      String javaHome = javaHomeReviser.apply(getJavaHome());
      if (javaHome != null) {
        commands.add(javaHome);
      }

      List<String> jvmArgs = jvmArgsReviser.apply(new ArrayList<>(getJvmArgs()));
      jvmArgs.stream().filter(Objects::isNotNull).forEach(commands::add);

      if (mode == LaunchMode.FORK_DEBUG || mode == LaunchMode.NORMAL_DEBUG
          || mode == LaunchMode.RESTART_DEBUG) {
        String debug = debugArgsReviser.apply(getDebugArgs());
        if (debug != null) {
          commands.add(debug);
        }
      }

      List<String> classPathArgs =
          defaultObject(classPathArgsReviser.apply(getClassPathArgs(null)), emptyList());
      classPathArgs.stream().filter(Objects::isNotNull).forEach(commands::add);

      List<String> mainCommands =
          defaultObject(mainCommandsReviser.apply(getMainCommands()), emptyList());
      mainCommands.stream().filter(Objects::isNotNull).forEach(commands::add);

      final Process process;
      ProcessBuilder builder = new ProcessBuilder();
      builder.command(commands);
      switch (mode) {
        case FORK:
        case FORK_DEBUG: {
          builder.inheritIO();
          process = builder.start();
          try {
            process.waitFor();
          } catch (InterruptedException e) {
            throw new CorantRuntimeException(e);
          }
          break;
        }
        case RESTART:
        case RESTART_DEBUG:
          process = builder.start();
          System.exit(0);
          break;
        default:
          process = builder.start();
          break;
      }
      return process;
    }

    public Launcher mainCommandsReviser(final UnaryOperator<List<String>> mainCommandsReviser) {
      if (mainCommandsReviser != null) {
        this.mainCommandsReviser = mainCommandsReviser;
      }
      return this;
    }

    public Launcher mode(final LaunchMode mode) {
      this.mode = defaultObject(mode, LaunchMode.NORMAL);
      return this;
    }
  }

  public enum LaunchMode {
    NORMAL, NORMAL_DEBUG, FORK, FORK_DEBUG, RESTART, RESTART_DEBUG;
  }
}

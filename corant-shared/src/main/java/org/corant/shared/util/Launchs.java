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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 上午10:37:10
 *
 */
public class Launchs {

  private final static Pattern debugPattern = Pattern.compile("-Xdebug|jdwp");

  private Launchs() {
    super();
  }

  public static void debugAs() throws IOException, InterruptedException {
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

  public static void deregisterFromMBean(String... objectNamings) {
    if (isEmpty(objectNamings)) {
      return;
    }
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    for (String objectNaming : objectNamings) {
      if (isNotBlank(objectNaming)) {
        ObjectName objectName = null;
        try {
          objectName = new ObjectName(objectNaming);
        } catch (MalformedObjectNameException ex) {
          throw new CorantRuntimeException(ex);
        }
        try {
          if (server.isRegistered(objectName)) {
            server.unregisterMBean(objectName);
          }
        } catch (MBeanRegistrationException | InstanceNotFoundException ex) {
          throw new CorantRuntimeException(ex);
        }
      }
    }
  }

  public static List<String> getClassPathArgs(Predicate<String> filter) {
    List<String> args = new ArrayList<>();
    args.add("-classpath");
    args.add(getClassPaths().stream().filter(cp -> filter == null ? true : filter.test(cp))
        .collect(Collectors.joining(File.pathSeparator)));
    return args;
  }

  public static List<String> getClassPaths() {
    return Arrays.stream(split(System.getProperty("java.class.path"), File.pathSeparator))
        .filter(Strings::isNotBlank).collect(Collectors.toList());
  }

  public static String getDebugArgs() {
    return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000";
  }

  public static Long getFreeMemoryMb() {
    return Runtime.getRuntime().freeMemory() / Defaults.ONE_MB;
  }

  public static String getJavaHome() {
    return Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java").toString();
  }

  public static String getJavaVersion() {
    return System.getProperty("java.version");
  }

  public static List<String> getJvmArgs() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments();
  }

  public static List<String> getMainCommands() {
    return Arrays.stream(split(System.getProperty("sun.java.command"), " "))
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

  public static void registerToMBean(String objectNaming, Object object) {
    if (object == null || isBlank(objectNaming)) {
      return;
    }
    ObjectName objectName = null;
    try {
      objectName = new ObjectName(objectNaming);
    } catch (MalformedObjectNameException ex) {
      throw new CorantRuntimeException(ex);
    }

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      if (server.isRegistered(objectName)) {
        server.unregisterMBean(objectName);
      }
      server.registerMBean(object, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException
        | NotCompliantMBeanException | InstanceNotFoundException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static void runAs() throws IOException, InterruptedException {
    List<String> commands = new ArrayList<>();
    commands.add(getJavaHome());
    commands.addAll(getJvmArgs());
    commands.addAll(getClassPathArgs(null));
    commands.addAll(getMainCommands());
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(commands);
    builder.inheritIO();
    Process process = builder.start();
    process.waitFor();
  }
}

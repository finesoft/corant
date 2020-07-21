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
package org.corant.kernel.boot;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Threads.tryThreadSleep;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Launchs;
import org.corant.shared.util.UnsafeAccessors;

/**
 * corant-kernel
 *
 * <p>
 * Simple use of shared memory(MMF IPC) to handle application startup, start, stop, restart and
 * shutdown.
 * </p>
 *
 * <p>
 * <b>Argument command description:</b>
 * </p>
 * <ul>
 * <li>
 * <p>
 * All arguments related to startup or shutdown are the last in the arguments list and start with
 * '-', if no argument list is provided, it is regarded as executing the startup command
 * </p>
 * </li>
 * <li>
 * <p>
 * Argument '-startup' use to create new process and launch the application.
 * </p>
 * </li>
 * <li>
 * <p>
 * Argument '-start' use to startup the application(startup the application CDI container, reload
 * configurations etc), it don't create new process, it was ignored if the current application was
 * started or the application process wasn't created.
 * </p>
 * </li>
 * <li>
 * <p>
 * Argument '-stop' use to stop the application(stop the application CDI container, release
 * configurations etc), it don't create new process, it was ignored if the current application was
 * stopped or the application process wasn't created.
 * </p>
 * </li>
 * <li>
 * <p>
 * Argument '-restart' use to restart the application(stop and start the application CDI container,
 * release and reload configurations etc), it don't create new process, it was ignored if the
 * application process wasn't created.
 * </p>
 * </li>
 * <li>
 * <p>
 * Argument '-shutdown' use to shutdown the application(stop the application CDI container, release
 * configurations etc), it will exit and end the application process, it was ignored if the
 * application process wasn't created.
 * </p>
 * </li>
 * </ul>
 *
 * <p>
 * <b>NOTE:</b><br/>
 * When starting multiple application instances in the same system, the execution of related
 * commands may cause the reaction of multiple instances, because we use MMF. In addition to the
 * startup command, if other commands are followed by '-pid', where the 'pid' is the process id, it
 * means that the command is only propagated to the specified process, if there is no '-pid' behind
 * it, it means that the command will be propagated to all involved to the processes.
 * </p>
 *
 * @author bingo 下午3:32:02
 *
 */
public class DirectRunner {
  static final Path MMF_DIR = Defaults.corantUserDir("-runner");
  static final String MMF_IPCF_PREFIX = ".ipc";
  static final byte SIGNAL_START = 0;
  static final byte SIGNAL_STOP = 1;
  static final byte SIGNAL_RESTART = 2;
  static final byte SIGNAL_SHUTDOWN = 3;
  static final String COMMAND_STARTUP = "startup";
  static final String COMMAND_START = "start";
  static final String COMMAND_STOP = "stop";
  static final String COMMAND_RESTART = "restart";
  static final String COMMAND_SHUTDOWN = "shutdown";
  static final String COMMAND_SPLITOR = "-";

  static {
    File dir = MMF_DIR.toFile();
    if (!dir.exists()) {
      shouldBeTrue(dir.mkdirs(), "Can't make dir for %s.", MMF_DIR.toString());
    }
  }

  protected String[] arguments = new String[0];

  protected DirectRunner(String[] arguments) {
    if (arguments != null) {
      this.arguments = arguments;
    }
  }

  public static void main(String... args) {
    if (isEmpty(args)) {
      new DirectRunner(args).perform(null);
    } else {
      String cmd = defaultString(args[args.length - 1]);
      if (cmd.startsWith(COMMAND_SPLITOR)) {
        new DirectRunner(args).perform(cmd.substring(1));
      }
    }
  }

  protected synchronized void await() throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(currentCtrlPath().toFile(), "rw");
        FileChannel fc = raf.getChannel();) {
      final MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 8);
      byte lastState = SIGNAL_START;
      for (;;) {
        if (mbb.hasRemaining()) {
          byte state = mbb.get();
          if (lastState != state) {
            lastState = state;
            if (state == SIGNAL_STOP) {
              stop(false);
            } else if (state == SIGNAL_START) {
              start(false);
            } else if (state == SIGNAL_RESTART) {
              stop(false);
              start(false);
            } else if (state == SIGNAL_SHUTDOWN) {
              stop(true);
              break;
            }
          }
          tryThreadSleep(1000L);
          mbb.position(0);
        }
        tryThreadSleep(1000L);
      }
      UnsafeAccessors.free(mbb);
    } finally {
      currentCtrlPath().toFile().deleteOnExit();
    }
  }

  protected synchronized void perform(String cmd) {
    if (isBlank(cmd) || cmd.startsWith(COMMAND_STARTUP)) {
      startup();
    } else {
      String[] cmds = split(cmd, COMMAND_SPLITOR, true, true);
      if (cmds.length == 1) {
        File[] files = MMF_DIR.toFile().listFiles(f -> f.getName().startsWith(MMF_IPCF_PREFIX));
        if (files == null) {
          return;
        }
        for (File file : files) {
          perform(cmds[0], file);
        }
      } else {
        File[] files = MMF_DIR.toFile().listFiles(f -> f.getName().startsWith(MMF_IPCF_PREFIX));
        if (files == null) {
          return;
        }
        String suffix = COMMAND_SPLITOR.concat(cmds[1]);
        for (File file : files) {
          if (file.getName().endsWith(suffix)) {
            perform(cmds[0], file);
          }
        }
      }
    }
  }

  protected synchronized void perform(String cmd, File file) {
    try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel fc = raf.getChannel();) {
      MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 8);
      mbb.clear();
      if (COMMAND_RESTART.equalsIgnoreCase(cmd)) {
        mbb.put(SIGNAL_RESTART);
      } else if (COMMAND_STOP.equalsIgnoreCase(cmd)) {
        mbb.put(SIGNAL_STOP);
      } else if (COMMAND_SHUTDOWN.equalsIgnoreCase(cmd)) {
        mbb.put(SIGNAL_SHUTDOWN);
      } else if (COMMAND_START.equalsIgnoreCase(cmd)) {
        mbb.put(SIGNAL_START);
      } else {
        System.err.println(String.format("Command [%s] illegality!", cmd));
      }
      UnsafeAccessors.free(mbb);
    } catch (Exception e) {
      e.printStackTrace();
      throw new CorantRuntimeException(e);
    }
  }

  protected synchronized void start(boolean await) {
    try {
      if (Corant.current() == null) {
        Corant.startup(arguments);
        if (await) {
          await();
        }
      } else if (!Corant.current().isRuning()) {
        Corant.current().start(Functions.emptyConsumer());
      }
    } catch (Exception t) {
      t.printStackTrace();
      throw new CorantRuntimeException("Can't start corant! please check logging.");
    }
  }

  protected synchronized void startup() {
    try {
      Files.deleteIfExists(currentCtrlPath());
    } catch (IOException e) {
      e.printStackTrace();
      throw new CorantRuntimeException(e);
    }
    start(true);
  }

  protected synchronized void stop(boolean exist) {
    try {
      if (exist) {
        Corant.shutdown();
      } else {
        Corant.current().stop();
      }
    } catch (Exception t) {
      t.printStackTrace();
      throw new CorantRuntimeException("Can't stop corant! please check logging.");
    }
  }

  Path currentCtrlPath() {
    return MMF_DIR.resolve(MMF_IPCF_PREFIX.concat(COMMAND_SPLITOR).concat(Launchs.getPid()));
  }

}

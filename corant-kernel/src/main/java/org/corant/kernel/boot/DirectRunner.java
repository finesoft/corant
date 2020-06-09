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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.tryThreadSleep;
import static org.corant.shared.util.StringUtils.defaultString;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.corant.shared.util.ObjectUtils;

/**
 * corant-kernel
 *
 * Simple use of shared memory(MMF IPC) to handle application startup, start, stop, restart and
 * shutdown.
 *
 * <p>
 * <ul>
 * <b>Argument command description:</b>
 * <li>All arguments related to startup or shutdown are the last in the arguments list and start
 * with '-', if no argument list is provided, it is regarded as executing the startup command</li>
 * <li>Argument '-startup' use to create new process and launch the application.</li>
 * <li>Argument '-start' use to startup the application(startup the application CDI container,
 * reload configurations etc), it don't create new process, it was ignored if the current
 * application was started or the application process wasn't created.</li>
 * <li>Argument '-stop' use to stop the application(stop the application CDI container, release
 * configurations etc), it don't create new process, it was ignored if the current application was
 * stopped or the application process wasn't created.</li>
 * <li>Argument '-restart' use to restart the application(stop and start the application CDI
 * container, release and reload configurations etc), it don't create new process, it was ignored if
 * the application process wasn't created.</li>
 * <li>Argument '-shutdown' use to shutdown the application(stop the application DI container,
 * release configurations etc), it will quit the application process, it was ignored if the
 * application process wasn't created.</li>
 * <ul>
 * </p>
 *
 * <p>
 * NOTE: When starting multiple application instances in the same system, the execution of related
 * commands may cause the reaction of multiple instances, because we use MMF. This may be changed in
 * the future.
 *
 * @author bingo 下午3:32:02
 *
 */
public class DirectRunner {
  static final String MMF_IPC = Defaults.corantUserDir("-mmf").resolve(".ipc").toString();
  static final byte SIGNAL_START = 0;
  static final byte SIGNAL_STOP = 1;
  static final byte SIGNAL_RESTART = 2;
  static final byte SIGNAL_SHUTDOWN = 3;
  static final String COMMAND_STARTUP = "startup";
  static final String COMMAND_START = "start";
  static final String COMMAND_STOP = "stop";
  static final String COMMAND_RESTART = "restart";
  static final String COMMAND_SHUTDOWN = "shutdown";

  private String[] arguments = new String[0];

  public static void main(String... args) {
    if (isEmpty(args)) {
      new DirectRunner().launch(null);
    } else {
      String cmd = defaultString(args[args.length - 1]);
      if (cmd.startsWith("-")) {
        new DirectRunner().launch(cmd.substring(1));
      }
    }
  }

  synchronized void await() {
    try (RandomAccessFile raf = new RandomAccessFile(new File(MMF_IPC), "rw");
        FileChannel fc = raf.getChannel();) {
      final MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, 8);
      byte lastState = SIGNAL_START;
      for (;;) {
        if (mem.hasRemaining()) {
          byte state = mem.get();
          if (lastState != state) {
            lastState = state;
            if (state == SIGNAL_STOP) {
              stop();
            } else if (state == SIGNAL_START) {
              start(false);
            } else if (state == SIGNAL_RESTART) {
              stop();
              start(false);
            } else if (state == SIGNAL_SHUTDOWN) {
              stop();
              break;
            }
          }
          tryThreadSleep(1000L);
          mem.position(0);
        }
        tryThreadSleep(1000L);
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  synchronized boolean isRunning() {
    try {
      return Corant.current() != null && Corant.current().isRuning();
    } catch (Exception t) {
      throw new RuntimeException("Can't check corant running! please check logging.");
    }
  }

  synchronized void launch(String cmd) {
    try (RandomAccessFile raf = new RandomAccessFile(resolveMmfIpc(), "rw");
        FileChannel fc = raf.getChannel();) {
      MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_WRITE, 0, 8);
      mem.clear();
      if (COMMAND_RESTART.equalsIgnoreCase(cmd)) {
        mem.put(SIGNAL_RESTART);
      } else if (COMMAND_STOP.equalsIgnoreCase(cmd)) {
        mem.put(SIGNAL_STOP);
      } else if (COMMAND_SHUTDOWN.equalsIgnoreCase(cmd)) {
        mem.put(SIGNAL_SHUTDOWN);
      } else if (COMMAND_START.equalsIgnoreCase(cmd)) {
        mem.put(SIGNAL_START);
      } else {
        start(true);
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  synchronized void start(boolean await) {
    try {
      if (Corant.current() == null) {
        Corant.run(new Class[0], arguments);
        if (await) {
          await();
        }
      } else if (!isRunning()) {
        Corant.current().start(ObjectUtils.emptyConsumer());
      }
    } catch (Exception t) {
      throw new RuntimeException("Can't start corant! please check logging.");
    }
  }

  synchronized void stop() {
    try {
      if (isRunning()) {
        Corant.current().stop();
      }
    } catch (Exception t) {
      throw new RuntimeException("Can't stop corant! please check logging.");
    }
  }

  private File resolveMmfIpc() {
    File f = new File(MMF_IPC);
    f.delete();
    if (!f.getParentFile().exists()) {
      f.getParentFile().mkdirs();
    }
    return f;
  }
}

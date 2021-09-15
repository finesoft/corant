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
package org.corant.shared.resource.watch;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.File;
import java.nio.file.Path;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Threads;

/**
 * corant-shared
 *
 * @author bingo 下午9:44:25
 *
 */
public class Watchers {

  public static final String DAEMON_THREAD_PERFIX = Names.CORANT.concat("-fsw-dae-");

  public static Watcher watchDirectoryInDaemon(File fileDir, boolean recursive,
      FileChangeListener... listeners) {
    shouldBeTrue(fileDir != null && fileDir.exists() && fileDir.isDirectory());
    final Watcher watcher = new DirectoryWatcher(fileDir, recursive, listeners);
    Threads.runDaemon(DAEMON_THREAD_PERFIX.concat(fileDir.getName()), watcher);
    return watcher;
  }

  public static Watcher watchInDaemon(File fileDir, FileChangeListener... listeners) {
    shouldBeTrue(fileDir != null && fileDir.exists());
    final Watcher watcher = fileDir.isFile() ? new FileWatcher(fileDir, -1, listeners)
        : new DirectoryWatcher(fileDir, true, listeners);
    Threads.runDaemon(DAEMON_THREAD_PERFIX.concat(fileDir.getName()), watcher);
    return watcher;
  }

  public static Watcher watchInDaemon(Path dir, FileChangeListener... listeners) {
    Path path = shouldNotNull(dir).normalize();
    final Watcher watcher = dir.toFile().isFile() ? new FileWatcher(dir.toFile(), -1, listeners)
        : new DirectoryWatcher(dir, true, listeners);
    final String name = path.getName(path.getNameCount() - 1).toString();
    Threads.runDaemon(DAEMON_THREAD_PERFIX.concat(name), watcher);
    return watcher;
  }

}

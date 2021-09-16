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
import java.util.function.Predicate;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Threads;

/**
 * corant-shared
 *
 * @author bingo 下午9:44:25
 *
 */
public class Watchers {

  public static final String DAEMON_THREAD_PREFIX = Names.CORANT.concat("-fsw-dae-");

  public static Watcher watchDirectoryInDaemon(File fileDir, boolean recursive,
      Predicate<Path> filter, FileChangeListener listeners) {
    shouldBeTrue(fileDir != null && fileDir.exists() && fileDir.isDirectory(),
        "The file dir to be watched can't null and must be a directory.");
    final Watcher watcher = new DirectoryWatcher(fileDir, recursive, filter, listeners);
    Threads.runInDaemon(DAEMON_THREAD_PREFIX.concat(fileDir.getName()), watcher);
    return watcher;
  }

  public static Watcher watchFileInDaemon(File file, long pollingIntervalMs,
      FileChangeListener listeners) {
    shouldBeTrue(file != null && file.exists() && file.isFile(),
        "The file to be watched can't null and must not a directory.");
    final Watcher watcher = new FileWatcher(file, pollingIntervalMs, listeners);
    Threads.runInDaemon(DAEMON_THREAD_PREFIX.concat(file.getName()), watcher);
    return watcher;
  }

  public static Watcher watchInDaemon(File fileOrDir, FileChangeListener listeners) {
    shouldBeTrue(fileOrDir != null && fileOrDir.exists(), "The file dir to be watched can't null.");
    final Watcher watcher = fileOrDir.isFile() ? new FileWatcher(fileOrDir, -1, listeners)
        : new DirectoryWatcher(fileOrDir, true, null, listeners);
    Threads.runInDaemon(DAEMON_THREAD_PREFIX.concat(fileOrDir.getName()), watcher);
    return watcher;
  }

  public static Watcher watchInDaemon(Path dir, FileChangeListener listeners) {
    Path path = shouldNotNull(dir, "The file dir to be watched can't null.").normalize();
    final Watcher watcher = dir.toFile().isFile() ? new FileWatcher(dir.toFile(), -1, listeners)
        : new DirectoryWatcher(dir, true, null, listeners);
    final String name = path.getName(path.getNameCount() - 1).toString();
    Threads.runInDaemon(DAEMON_THREAD_PREFIX.concat(name), watcher);
    return watcher;
  }

}

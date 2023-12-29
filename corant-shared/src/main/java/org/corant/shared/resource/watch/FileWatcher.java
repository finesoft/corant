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

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.corant.shared.util.Threads;

/**
 * corant-shared
 *
 * @author bingo 下午9:43:59
 */
public class FileWatcher extends AbstractWatcher {

  protected final File file;
  protected final AtomicLong modifiedTimeStamp;
  protected final long pollingIntervalMs;

  public FileWatcher(File file, long pollingIntervalMs, FileChangeListener... listeners) {
    shouldBeTrue(file != null && file.isFile(), "The file to be watched must be a non null file.");
    this.pollingIntervalMs = pollingIntervalMs < 0 ? 64 : pollingIntervalMs;
    Collections.addAll(this.listeners, listeners);
    this.file = file;
    modifiedTimeStamp = new AtomicLong(file.lastModified());
  }

  @Override
  public void run() {
    while (running) {
      if (!file.exists()) {
        fire(ENTRY_DELETE, file, null);
        break;
      } else {
        final long lastModified = file.lastModified();
        if (lastModified > modifiedTimeStamp.getAndSet(lastModified)) {
          fire(ENTRY_MODIFY, file, null);
        }
      }
      if (pollingIntervalMs > 0) {
        Threads.tryThreadSleep(pollingIntervalMs);
      }
    }
    running = false;
  }

}

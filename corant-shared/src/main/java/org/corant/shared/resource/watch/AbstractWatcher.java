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

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.io.File;
import java.io.IOException;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * corant-shared
 *
 * @author bingo 下午9:48:27
 *
 */
public abstract class AbstractWatcher implements Watcher {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  protected volatile boolean running = true;
  protected final List<FileChangeListener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public void clearListeners() {
    listeners.clear();
  }

  @Override
  public void close() throws IOException {
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean registerListener(FileChangeListener... listeners) {
    boolean registered = false;
    for (FileChangeListener listener : listeners) {
      registered |= this.listeners.add(listener);
    }
    return registered;
  }

  @Override
  public boolean removeListener(FileChangeListener listener) {
    return listeners.remove(listener);
  }

  protected void fire(WatchEvent.Kind<?> kind, File file, File prev) {
    final File eventFile = file;
    final File eventPrevFile = prev;
    for (FileChangeListener listener : listeners) {
      try {
        if (prev == null) {
          listener.onChange(new FileChangeEvent(kind, eventFile));
        } else {
          listener.onChange(new PathUpdateEvent(ENTRY_MODIFY, eventFile, eventPrevFile));
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "Occurred error on fire file change event!");
      }
    }
  }
}

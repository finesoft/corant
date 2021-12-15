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

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午6:08:44
 *
 */
public class DirectoryWatcher extends AbstractWatcher {

  protected final WatchService service;
  protected final Map<WatchKey, Path> keys;
  protected final boolean recursive;
  protected final Predicate<Path> filter;
  protected Path path;
  protected volatile boolean trace;

  public DirectoryWatcher(File fileDir, boolean recursive, Predicate<Path> filter,
      FileChangeListener... listeners) {
    this(shouldNotNull(fileDir, "The file dir to be watched can't null!").toPath(), recursive,
        filter, listeners);
  }

  public DirectoryWatcher(Path dir, boolean recursive, Predicate<Path> pathFilter,
      FileChangeListener... listeners) {
    try {
      path = shouldNotNull(dir, "The path to be watched can't null!", dir).normalize();
      filter = defaultObject(pathFilter, p -> true);
      service = FileSystems.getDefault().newWatchService();
      keys = new HashMap<>();
      Collections.addAll(this.listeners, listeners);
      if (path.toFile().isFile()) {
        this.recursive = true;
        path = path.getParent();
      } else {
        this.recursive = recursive;
      }
      if (this.recursive) {
        registerAll(path);
      } else {
        register(path);
      }
      trace = true;
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      if (service != null) {
        service.close();
      }
    } catch (Throwable t) {
      logger.log(Level.WARNING, t, () -> "Close watch service occurred error!");
    } finally {
      logger.info("Close the watcher!");
    }
    super.close();
  }

  /**
   * Process all events for keys queued to the watcher
   */
  @Override
  public void run() {
    while (running) {
      try {
        WatchKey key = service.take();
        Path dir = keys.get(key);
        if (dir == null) {
          logger.warning("WatchKey not recognized!");
          continue;
        }
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == OVERFLOW) {
            // fire the events
            fire(kind, null, null);
            continue;
          }
          // Context for directory entry event is the file name of entry
          WatchEvent<Path> ev = forceCast(event);
          Path name = ev.context();
          Path child = dir.resolve(name);
          // fire the events
          if (filter.test(child)) {
            fire(event.kind(), child.toFile(), null);
          }
          // if directory is created, and watching recursively, then register it and its
          // sub-directories
          if (recursive && kind == ENTRY_CREATE) {
            try {
              if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                registerAll(child);
              }
            } catch (IOException x) {
              logger.log(Level.WARNING, x,
                  () -> "Register directory occurred error while event polling!");
            }
          }
        }

        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
          keys.remove(key);
          // all directories are inaccessible
          if (keys.isEmpty()) {
            break;
          }
        }
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e, () -> "Watch service was interrupted!");
        break;
      } catch (ClosedWatchServiceException cwse) {
        logger.warning(() -> "Watch service was closed!");
        break;
      }
    }
    running = false;
  }

  /**
   * Register the given directory with the WatchService
   */
  protected void register(Path dir) throws IOException {
    WatchKey key = dir.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    if (trace) {
      Path prev = keys.get(key);
      if (prev == null) {
        logger.fine(() -> String.format("register %s to watch service.", dir));
      } else if (!dir.equals(prev)) {
        logger.fine(() -> String.format("update %s -> %s", prev, dir));
        fire(null, dir.toFile(), prev.toFile());
      }
    }
    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the WatchService.
   */
  protected void registerAll(final Path start) throws IOException {
    Files.walkFileTree(start, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

}

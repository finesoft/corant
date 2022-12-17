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
package org.corant.modules.bundle;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-bundle
 *
 * @author bingo 上午10:16:05
 *
 */
@ApplicationScoped
public class DefaultMessageSourceManager implements MessageSourceManager {

  protected final ReadWriteLock rwl = new ReentrantReadWriteLock();

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<MessageSource> messageSources;

  @Override
  public void refresh() {
    Lock writeLock = rwl.writeLock();
    try {
      writeLock.lock();
      if (!messageSources.isUnsatisfied()) {
        messageSources.stream().sorted(Sortable::compare).forEach(MessageSource::refresh);
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void release() {
    Lock writeLock = rwl.writeLock();
    try {
      writeLock.lock();
      if (!messageSources.isUnsatisfied()) {
        messageSources.stream().sorted(Sortable::compare).forEach(t -> {
          try {
            t.close();
          } catch (Exception e) {
            throw new CorantRuntimeException(e);
          }
        });
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Stream<MessageSource> stream() {
    Lock readLock = rwl.readLock();
    try {
      readLock.lock();
      if (!messageSources.isUnsatisfied()) {
        return messageSources.stream().sorted(Sortable::compare);
      }
    } finally {
      readLock.unlock();
    }
    return MessageSourceManager.super.stream();
  }

  @PostConstruct
  protected void onPostConstruct() {
    refresh();
  }
}

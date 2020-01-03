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
package org.corant.suites.ddd.unitwork;

import static org.corant.suites.cdi.Instances.find;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;
import org.corant.suites.ddd.message.MessageDispatcher;
import org.corant.suites.ddd.message.MessageStorage;
import org.corant.suites.ddd.saga.SagaService;
import org.corant.suites.jpa.shared.PersistenceService;

/**
 * corant-suites-ddd
 *
 * @author bingo 上午11:51:01
 */
public interface UnitOfWorksManager {

  @SuppressWarnings("unchecked")
  static <U extends UnitOfWork> Optional<U> currentUnitOfWork(Annotation... annotations) {
    Optional<UnitOfWorksManager> uowm = find(UnitOfWorksManager.class, annotations);
    return Optional.ofNullable(uowm.isPresent() ? (U) uowm.get().getCurrentUnitOfWork() : null);
  }

  UnitOfWork getCurrentUnitOfWork();

  Stream<UnitOfWorksHandler> getHandlers();

  Stream<UnitOfWorksListener> getListeners();

  MessageDispatcher getMessageDispatcher();

  MessageStorage getMessageStorage();

  PersistenceService getPersistenceService();

  SagaService getSagaService();

  @FunctionalInterface
  public interface UnitOfWorksHandler {

    static int compare(UnitOfWorksHandler h1, UnitOfWorksHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    void onPreComplete(UnitOfWork uow);
  }

  @FunctionalInterface
  public interface UnitOfWorksListener {
    void onCompleted(Object registration, boolean success);
  }

}

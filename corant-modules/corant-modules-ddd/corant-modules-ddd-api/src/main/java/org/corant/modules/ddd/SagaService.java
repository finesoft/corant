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
package org.corant.modules.ddd;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 下午3:48:30
 *
 */
public interface SagaService {

  static SagaService empty() {
    return EmptySagaService.INSTANCE;
  }

  Stream<SagaManager> getManagers(Annotation... annotations);

  void persist(Saga saga);

  default void prepare() {}

  void trigger(Message message);

  class EmptySagaService implements SagaService {

    public static final SagaService INSTANCE = new EmptySagaService();

    protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public Stream<SagaManager> getManagers(Annotation... annotations) {
      logger.fine(
          () -> "The saga service is an empty implementation that does not really implement persistence");
      return Stream.empty();
    }

    @Override
    public void persist(Saga saga) {
      logger.fine(
          () -> "The saga service is an empty implementation that does not really implement persistence");
    }

    @Override
    public void trigger(Message message) {
      logger.fine(
          () -> "The saga service is an empty implementation that does not really implement trigger");
    }
  }

  interface SagaManager {

    Saga begin(Message message);

    void end(Message message);

    Saga get(String queue, String trackingToken);

    List<Saga> select(AggregateIdentifier aggregateIdentifier);
  }
}

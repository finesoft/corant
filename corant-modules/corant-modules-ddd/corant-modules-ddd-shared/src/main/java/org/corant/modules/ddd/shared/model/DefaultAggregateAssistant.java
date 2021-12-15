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
package org.corant.modules.ddd.shared.model;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Preconditions.requireNotNull;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;
import org.corant.context.CDIs;
import org.corant.modules.bundle.GlobalMessageCodes;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.AggregateAssistant;
import org.corant.modules.ddd.Event;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.UnitOfWork;
import org.corant.modules.ddd.shared.unitwork.UnitOfWorks;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * Use for the collaboration between Aggregate Root and external, such as fire events
 * enqueue/dequeue messages.
 * </p>
 *
 * @author bingo 上午10:57:03
 *
 */
public class DefaultAggregateAssistant implements AggregateAssistant {

  private static final String FIRE_LOG = "Fire event [%s] to event listener!";
  private static final String RISE_LOG = "Register integration message [%s] to message queue!";

  protected final Logger logger = Logger.getLogger(this.getClass().toString());
  protected final Aggregate aggregate;
  protected final Queue<Message> messages = new LinkedList<>();

  public DefaultAggregateAssistant(Aggregate aggregate) {
    this.aggregate = requireNotNull(aggregate, GlobalMessageCodes.ERR_PARAM);
  }

  @Override
  public void clearMessages() {
    messages.clear();
  }

  @Override
  public Optional<? extends UnitOfWork> currentUnitOfWork() {
    return resolve(UnitOfWorks.class).currentDefaultUnitOfWork();
  }

  @Override
  public List<Message> dequeueMessages(boolean flush) {
    List<Message> exMsgs = new LinkedList<>(messages);
    if (flush) {
      clearMessages();
    }
    return exMsgs;
  }

  @Override
  public void enqueueMessages(boolean anyway, Message... messages) {
    if (aggregate.getId() == null || anyway) {
      Optional<? extends UnitOfWork> uow = currentUnitOfWork();
      if (uow.isPresent()) {
        for (Message msg : messages) {
          if (msg != null) {
            logger.fine(() -> String.format(RISE_LOG, msg.toString()));
            uow.get().register(msg);
          }
        }
      } else {
        throw new CorantRuntimeException("The UnitOfWork not found! please check the implements!");
      }
    } else {
      for (Message msg : messages) {
        if (msg != null) {
          logger.fine(() -> String.format(RISE_LOG, msg.toString()));
          // MessageUtils.mergeToQueue(this.messages, msg);
          this.messages.add(msg);
        }
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    DefaultAggregateAssistant other = (DefaultAggregateAssistant) obj;
    if (aggregate == null) {
      return other.aggregate == null;
    } else {
      return aggregate.equals(other.aggregate);
    }
  }

  @Override
  public <U extends Event> CompletionStage<U> fireAsyncEvent(U event, Annotation... qualifiers) {
    shouldNotNull(event);
    logger.fine(() -> String.format(FIRE_LOG, event.toString()));
    return CDIs.fireAsyncEvent(event, qualifiers);
  }

  @Override
  public void fireEvent(Event event, Annotation... qualifiers) {
    shouldNotNull(event);
    logger.fine(() -> String.format(FIRE_LOG, event.toString()));
    CDIs.fireEvent(event, qualifiers);
  }

  @Override
  public Aggregate getAggregate() {
    return aggregate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (aggregate == null ? 0 : aggregate.hashCode());
  }

}

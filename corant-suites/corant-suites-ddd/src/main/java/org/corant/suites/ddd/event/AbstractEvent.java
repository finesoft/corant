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
package org.corant.suites.ddd.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import org.corant.suites.ddd.annotation.stereotype.Events;

/**
 * corant-suites-ddd
 *
 * @author bingo 上午11:26:08
 */
@Events
public abstract class AbstractEvent implements Event {

  private static final long serialVersionUID = -3841473508359865099L;

  private transient Object source;

  private final Instant occurredTime = Instant.now();

  public AbstractEvent(Object source) {
    this.source = source;
  }

  protected AbstractEvent() {}

  @Override
  public Instant getOccurredTime() {
    return occurredTime;
  }

  @Override
  public Object getSource() {
    return source;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [occurredTime=" + occurredTime + ", source=" + source
        + "]";
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}

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
package org.corant.suites.ddd.message;

import org.corant.suites.ddd.model.Aggregate;

/**
 * corant-suites-ddd
 *
 * @author bingo 上午10:18:59
 *
 */
public abstract class AbstractAggregateMessage implements Message {

  private static final long serialVersionUID = -2704628374745952353L;

  protected AggregateMessageMetadata metadata;

  public AbstractAggregateMessage(Aggregate aggregate) {
    metadata = new AggregateMessageMetadata(aggregate);
  }

  protected AbstractAggregateMessage() {

  }

  @Override
  public AggregateMessageMetadata getMetadata() {
    return metadata;
  }

}

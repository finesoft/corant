/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.ddd.model;

import java.util.List;
import java.util.stream.Stream;
import org.corant.suites.ddd.message.Message;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午2:05:56
 *
 */
public abstract class AbstractAuditableAggregation extends AbstractAggregation {

  private static final long serialVersionUID = 3636641230618671037L;

  public AbstractAuditableAggregation() {}

  public AbstractAuditableAggregation(Stream<? extends Message> messageStream) {}

  @Override
  public synchronized List<Message> extractMessages(boolean flush) {
    List<Message> events = super.extractMessages(flush);
    if (flush && !events.isEmpty()) {
      setVn(getVn() + events.size());
    }
    return events;
  }

}

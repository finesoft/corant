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
package org.corant.asosat.ddd.message;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.Message.MessageIdentifier;
import org.corant.suites.ddd.model.Value;

/**
 * @author bingo 下午6:29:15
 *
 */
@MappedSuperclass
@Embeddable
public abstract class AbstractMessageIdentifier implements MessageIdentifier, Value {

  private static final long serialVersionUID = -8217516556955390987L;

  @Column
  private String type;

  @Column
  private String queue;

  public AbstractMessageIdentifier() {}

  public AbstractMessageIdentifier(Message message) {
    this(message.getClass().getName(), message.getMetadata().getQueue().toString());
  }

  public AbstractMessageIdentifier(String type, String queue) {
    super();
    this.type = type;
    this.queue = queue;
  }

  @Override
  public String getQueue() {
    return queue;
  }

  @Override
  public String getType() {
    return type;
  }

  protected void setQueue(String queue) {
    this.queue = queue;
  }

  protected void setType(String type) {
    this.type = type;
  }



}

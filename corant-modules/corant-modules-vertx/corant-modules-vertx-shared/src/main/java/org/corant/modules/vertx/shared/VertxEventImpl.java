/*
 * JBoss, Home of Professional Open Source Copyright 2016, Red Hat, Inc., and individual
 * contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.corant.modules.vertx.shared;

import java.util.logging.Logger;
import javax.enterprise.inject.Vetoed;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

/**
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 */
@Vetoed
class VertxEventImpl implements VertxEvent {

  private static final Logger LOGGER = Logger.getLogger(VertxEventImpl.class.getName());

  private final EventBus eventBus;

  private final Message<Object> message;

  private Object reply;

  VertxEventImpl(Message<Object> message, EventBus eventBus) {
    this.eventBus = eventBus;
    this.message = message;
  }

  @Override
  public void fail(int code, String message) {
    throw new RecipientFailure(code, message);
  }

  @Override
  public String getAddress() {
    return message.address();
  }

  @Override
  public MultiMap getHeaders() {
    return message.headers();
  }

  @Override
  public Object getMessageBody() {
    return message.body();
  }

  @Override
  public String getReplyAddress() {
    return message.replyAddress();
  }

  @Override
  public boolean isReplied() {
    return reply != null;
  }

  @Override
  public VertxMessage messageTo(String address) {
    return new VertxMessageImpl(address, eventBus);
  }

  @Override
  public boolean setReply(Object reply) {
    if (message.replyAddress() == null) {
      LOGGER.warning("The message was sent without a reply handler - the reply will be ignored");
    }
    if (this.reply != null) {
      LOGGER.warning("A reply was already set - the old value is replaced");
      return false;
    }
    this.reply = reply;
    return true;
  }

  Object getReply() {
    return reply;
  }

}

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

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.Message;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * Vertx {@link Message} wrapper.
 * <p>
 * An observer method must declare an event parameter of the type {@link VertxEvent} with
 * {@link VertxConsumer} qualifier in order to be notified when a message is sent via
 * {@link io.vertx.core.eventbus.EventBus}.
 * </p>
 *
 * @author Martin Kouba
 * @see VertxConsumer
 *
 */
public interface VertxEvent {

  /**
   * A failure code that is used if an observer method throws exception.
   *
   * @see Message#fail(int, String)
   */
  int OBSERVER_FAILURE_CODE = 0x1B00;

  /**
   * Aborts the processing of the event - no other observer methods will be called (unless the
   * thrown {@link RecipientFailure} is swallowed).
   *
   * @param code a failure code to pass back to the sender
   * @param message a failure message to pass back to the sender
   * @see Message#fail(int, String)
   */
  void fail(int code, String message);

  /**
   * @return the address the message was sends to
   * @see Message#address()
   */
  String getAddress();

  /**
   * @return the message headers
   * @see Message#headers()
   */
  MultiMap getHeaders();

  /**
   * @return the message body/payload
   * @see Message#body()
   */
  Object getMessageBody();

  /**
   * @return the reply address, or null in case of the message was sent without a reply handler
   * @see Message#replyAddress()
   */
  String getReplyAddress();

  /**
   * @return <code>true</code> if a reply was previously set, <code>false</code> otherwise
   * @see #setReply(Object)
   */
  boolean isReplied();

  /**
   * Send/publish messages using the Vertx event bus.
   *
   * @param address the address that message sends to
   * @return a message
   */
  VertxMessage messageTo(String address);

  /**
   * Set the reply to the message. Does not abort the processing of the event - other observer
   * methods will be notified. The first reply set is passed to {@link Message#reply(Object)}.
   * <p>
   * If the reply address is null (point-to-point messaging without reply handler) the reply is
   * ignored.
   * </p>
   * <p>
   * An observer is encouraged to call {@link #fail(int, String)} if it needs to set the reply and
   * this method returns <code>false</code>.
   * </p>
   *
   * @param reply the message to reply with.
   * @see Message#reply(Object)
   * @return <tt>true</tt> if the reply was successfully set
   */
  boolean setReply(Object reply);
}

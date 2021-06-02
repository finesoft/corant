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
package org.corant.modules.jms.metadata;

import static org.corant.shared.util.Annotations.EMPTY_ARRAY;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.newArrayList;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.corant.modules.jms.annotation.MessageDriven;
import org.corant.shared.util.Retry.BackoffAlgorithm;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:12:52
 *
 */
public class MessageDrivenMetaData {

  private final Class<?> beanClass;

  private final Annotation[] beanQualifiers;

  private final Method beanMethod;

  private final int acknowledge;

  private final BackoffAlgorithm brokenBackoffAlgo;

  private final double brokenBackoffFactor;

  private final String brokenDuration;

  private final int cacheLevel;

  private final int failureThreshold;

  private final long loopIntervalMs;

  private final String maxBrokenDuration;

  private final int receiveThreshold;

  private final long receiveTimeout;

  private final Collection<MessageReplyMetaData> reply;

  private final String selector;

  private final int tryThreshold;

  private final int txTimeout;

  private final boolean xa;

  public MessageDrivenMetaData(Class<?> beanClass, Annotation[] beanQualifiers, Method beanMethod,
      int acknowledge, BackoffAlgorithm brokenBackoffAlgo, double brokenBackoffFactor,
      String brokenDuration, int cacheLevel, int failureThreshold, long loopIntervalMs,
      String maxBrokenDuration, int receiveThreshold, long receiveTimeout,
      Collection<MessageReplyMetaData> reply, String selector, int tryThreshold, int txTimeout,
      boolean xa) {
    this.beanClass = beanClass;
    this.beanQualifiers =
        beanQualifiers == null ? EMPTY_ARRAY : Arrays.copyOf(beanQualifiers, beanQualifiers.length);
    this.beanMethod = beanMethod;
    this.acknowledge = acknowledge;
    this.brokenBackoffAlgo = brokenBackoffAlgo;
    this.brokenBackoffFactor = brokenBackoffFactor;
    this.brokenDuration = MetaDataPropertyResolver.get(brokenDuration);
    this.cacheLevel = cacheLevel;
    this.failureThreshold = failureThreshold;
    this.loopIntervalMs = loopIntervalMs;
    this.maxBrokenDuration = MetaDataPropertyResolver.get(maxBrokenDuration);
    this.receiveThreshold = receiveThreshold;
    this.receiveTimeout = receiveTimeout;
    this.reply = Collections.unmodifiableList(newArrayList(reply));
    this.selector = MetaDataPropertyResolver.get(selector);
    this.tryThreshold = tryThreshold;
    this.txTimeout = txTimeout;
    this.xa = xa;
  }

  public static MessageDrivenMetaData from(Method method, Annotation... qualifiers) {
    Method beanMethod = shouldNotNull(method);
    MessageDriven annotation = shouldNotNull(method.getAnnotation(MessageDriven.class));
    Class<?> beanClass = beanMethod.getDeclaringClass();
    Annotation[] beanQualifiers = qualifiers;
    int acknowledge = annotation.acknowledge();
    BackoffAlgorithm brokenBackoffAlgo = annotation.brokenBackoffAlgo();
    double brokenBackoffFactor = annotation.brokenBackoffFactor();
    String brokenDuration = annotation.brokenDuration();
    int cacheLevel = annotation.cacheLevel();
    int failureThreshold = annotation.failureThreshold();
    long loopIntervalMs = annotation.loopIntervalMs();
    String maxBrokenDuration = annotation.maxBrokenDuration();
    int receiveThreshold = annotation.receiveThreshold();
    long receiveTimeout = annotation.receiveTimeout();
    Set<MessageReplyMetaData> reply = MessageReplyMetaData.of(annotation.reply());
    String selector = annotation.selector();
    int tryThreshold = annotation.tryThreshold();
    int txTimeout = annotation.txTimeout();
    boolean xa = annotation.xa();
    return new MessageDrivenMetaData(beanClass, beanQualifiers, beanMethod, acknowledge,
        brokenBackoffAlgo, brokenBackoffFactor, brokenDuration, cacheLevel, failureThreshold,
        loopIntervalMs, maxBrokenDuration, receiveThreshold, receiveTimeout, reply, selector,
        tryThreshold, txTimeout, xa);
  }

  public int getAcknowledge() {
    return acknowledge;
  }

  public Class<?> getBeanClass() {
    return beanClass;
  }

  public Method getBeanMethod() {
    return beanMethod;
  }

  public Annotation[] getBeanQualifiers() {
    return Arrays.copyOf(beanQualifiers, beanQualifiers.length);
  }

  public BackoffAlgorithm getBrokenBackoffAlgo() {
    return brokenBackoffAlgo;
  }

  public double getBrokenBackoffFactor() {
    return brokenBackoffFactor;
  }

  public String getBrokenDuration() {
    return brokenDuration;
  }

  public int getCacheLevel() {
    return cacheLevel;
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public long getLoopIntervalMs() {
    return loopIntervalMs;
  }

  public String getMaxBrokenDuration() {
    return maxBrokenDuration;
  }

  public int getReceiveThreshold() {
    return receiveThreshold;
  }

  public long getReceiveTimeout() {
    return receiveTimeout;
  }

  public Collection<MessageReplyMetaData> getReply() {
    return reply;
  }

  public String getSelector() {
    return selector;
  }

  public int getTryThreshold() {
    return tryThreshold;
  }

  public int getTxTimeout() {
    return txTimeout;
  }

  public boolean isXa() {
    return xa;
  }

}

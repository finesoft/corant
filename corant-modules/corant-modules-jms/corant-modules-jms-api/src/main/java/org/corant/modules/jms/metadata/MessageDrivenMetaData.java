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

import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.get;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getBoolean;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getDouble;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getInt;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getLong;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getString;
import static org.corant.shared.util.Annotations.EMPTY_ARRAY;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.newArrayList;
import static org.corant.shared.util.Maps.immutableMapOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.corant.modules.jms.annotation.MessageDriven;
import org.corant.shared.retry.BackoffStrategy.BackoffAlgorithm;

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

  private final Map<String, String> specifiedSelectors;

  private final int tryThreshold;

  private final int txTimeout;

  private final boolean xa;

  public MessageDrivenMetaData(Class<?> beanClass, Annotation[] beanQualifiers, Method beanMethod,
      int acknowledge, BackoffAlgorithm brokenBackoffAlgo, double brokenBackoffFactor,
      String brokenDuration, int cacheLevel, int failureThreshold, long loopIntervalMs,
      String maxBrokenDuration, int receiveThreshold, long receiveTimeout,
      Collection<MessageReplyMetaData> reply, String selector, String[] specifiedSelectors,
      int tryThreshold, int txTimeout, boolean xa) {
    this.beanClass = beanClass;
    this.beanQualifiers =
        beanQualifiers == null ? EMPTY_ARRAY : Arrays.copyOf(beanQualifiers, beanQualifiers.length);
    this.beanMethod = beanMethod;
    this.acknowledge = acknowledge;
    this.brokenBackoffAlgo = brokenBackoffAlgo;
    this.brokenBackoffFactor = brokenBackoffFactor;
    this.brokenDuration = MetaDataPropertyResolver.get(brokenDuration, String.class);
    this.cacheLevel = cacheLevel;
    this.failureThreshold = failureThreshold;
    this.loopIntervalMs = loopIntervalMs;
    this.maxBrokenDuration = MetaDataPropertyResolver.get(maxBrokenDuration, String.class);
    this.receiveThreshold = receiveThreshold;
    this.receiveTimeout = receiveTimeout;
    this.reply = Collections.unmodifiableList(newArrayList(reply));
    this.selector = MetaDataPropertyResolver.get(selector, String.class);
    if (isNotEmpty(specifiedSelectors)) {
      this.specifiedSelectors = immutableMapOf((Object[]) specifiedSelectors);
    } else {
      this.specifiedSelectors = Collections.emptyMap();
    }
    this.tryThreshold = tryThreshold;
    this.txTimeout = txTimeout;
    this.xa = xa;
  }

  public static MessageDrivenMetaData from(Method method, Annotation... qualifiers) {
    Method beanMethod = shouldNotNull(method);
    MessageDriven annotation = shouldNotNull(method.getAnnotation(MessageDriven.class));
    return new MessageDrivenMetaData(beanMethod.getDeclaringClass(), qualifiers, beanMethod,
        getInt(annotation.acknowledge()),
        get(annotation.brokenBackoffAlgo(), BackoffAlgorithm.class),
        getDouble(annotation.brokenBackoffFactor()), getString(annotation.brokenDuration()),
        getInt(annotation.cacheLevel()), getInt(annotation.failureThreshold()),
        getLong(annotation.loopIntervalMs()), getString(annotation.maxBrokenDuration()),
        getInt(annotation.receiveThreshold()), getLong(annotation.receiveTimeout()),
        MessageReplyMetaData.of(annotation.reply()), getString(annotation.selector()),
        annotation.specifiedSelectors(), getInt(annotation.tryThreshold()),
        getInt(annotation.txTimeout()), getBoolean(annotation.xa()));
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

  public Map<String, String> getSpecifiedSelectors() {
    return specifiedSelectors;
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

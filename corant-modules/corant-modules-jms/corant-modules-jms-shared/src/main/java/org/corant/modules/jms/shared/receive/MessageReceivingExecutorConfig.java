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
package org.corant.modules.jms.shared.receive;

import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.isBlank;
import java.time.Duration;
import java.util.Map;
import org.corant.config.Configs;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.NamedObject;
import org.corant.modules.jms.receive.ManagedMessageReceivingExecutorConfig;
import org.corant.modules.jms.shared.AbstractJMSConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午7:17:04
 *
 */
@ConfigKeyRoot(value = "corant.jms", keyIndex = 2)
public class MessageReceivingExecutorConfig
    implements ManagedMessageReceivingExecutorConfig, NamedObject, DeclarativeConfig {

  private static final long serialVersionUID = -3439136173477457265L;

  public final static MessageReceivingExecutorConfig DFLT_INST =
      new MessageReceivingExecutorConfig();

  public final static Map<String, MessageReceivingExecutorConfig> CONFIGS =
      unmodifiableMap(Configs.resolveMulti(MessageReceivingExecutorConfig.class));

  @ConfigKeyItem(name = "allow-create-consumer-on-receiving-method", defaultValue = "false")
  protected boolean allowCreateConsumerOnReceivingMethod;

  @ConfigKeyItem(name = "allow-create-subscriber-on-receiving-method", defaultValue = "false")
  protected boolean allowCreateSubscriberOnReceivingMethod;

  @ConfigKeyItem(name = "allow-create-queue-on-receiving-method", defaultValue = "false")
  protected boolean allowCreateQueueOnReceivingMethod;

  @ConfigKeyItem(name = "allow-create-topic-on-receiving-method", defaultValue = "false")
  protected boolean allowCreateTopicOnReceivingMethod;

  @ConfigKeyItem(name = "allow-unsubscriber-on-receiving-method", defaultValue = "false")
  protected boolean allowUnsubscriberOnReceivingMethod;

  protected String connectionFactoryId;

  @ConfigKeyItem(name = "receive-task-initial-delay", defaultValue = "PT30S")
  protected Duration initialDelay = Duration.ofSeconds(30L);

  @ConfigKeyItem(name = "receive-task-delay", defaultValue = "PT1S")
  protected Duration delay = Duration.ofSeconds(1L);

  @ConfigKeyItem(name = "receive-executor-await-termination", defaultValue = "PT4S")
  protected Duration awaitTermination = Duration.ofSeconds(4L);

  @ConfigKeyItem(name = "receive-executor-cor-pool-size", defaultValue = "2")
  protected Integer corePoolSize = max(2, Runtime.getRuntime().availableProcessors());

  public static MessageReceivingExecutorConfig getExecutorConfig(AbstractJMSConfig config) {
    return CONFIGS.getOrDefault(config.getConnectionFactoryId(),
        MessageReceivingExecutorConfig.DFLT_INST);
  }

  public static MessageReceivingExecutorConfig getExecutorConfig(String connectionFactoryId) {
    return CONFIGS.getOrDefault(connectionFactoryId, MessageReceivingExecutorConfig.DFLT_INST);
  }

  @Override
  public Duration getAwaitTermination() {
    return awaitTermination;
  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  @Override
  public int getCorePoolSize() {
    return corePoolSize;
  }

  @Override
  public Duration getDelay() {
    return delay;
  }

  @Override
  public Duration getInitialDelay() {
    return initialDelay;
  }

  @Override
  public String getName() {
    return connectionFactoryId;
  }

  public boolean isAllowCreateConsumerOnReceivingMethod() {
    return allowCreateConsumerOnReceivingMethod;
  }

  public boolean isAllowCreateQueueOnReceivingMethod() {
    return allowCreateQueueOnReceivingMethod;
  }

  public boolean isAllowCreateSubscriberOnReceivingMethod() {
    return allowCreateSubscriberOnReceivingMethod;
  }

  public boolean isAllowCreateTopicOnReceivingMethod() {
    return allowCreateTopicOnReceivingMethod;
  }

  public boolean isAllowUnsubscriberOnReceivingMethod() {
    return allowUnsubscriberOnReceivingMethod;
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    if (isBlank(connectionFactoryId)) {
      connectionFactoryId = Qualifiers.resolveName(key);
    }
  }

}

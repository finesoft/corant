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

import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.isBlank;
import java.time.Duration;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.NamedObject;
import org.corant.modules.jms.receive.ManagedMessageReceivingExecutorConfig;
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

  protected String connectionFactoryId;

  @ConfigKeyItem(name = "receive-task-initial-delay", defaultValue = "PT30S")
  protected Duration initialDelay = Duration.ofSeconds(30L);

  @ConfigKeyItem(name = "receive-task-delay", defaultValue = "PT1S")
  protected Duration delay = Duration.ofSeconds(1L);

  @ConfigKeyItem(name = "receive-executor-await-termination", defaultValue = "PT4S")
  protected Duration awaitTermination = Duration.ofSeconds(4L);

  @ConfigKeyItem(name = "receive-executor-cor-pool-size", defaultValue = "2")
  protected Integer corePoolSize = max(2, Runtime.getRuntime().availableProcessors());

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

  @Override
  public void onPostConstruct(Config config, String key) {
    if (isBlank(connectionFactoryId)) {
      connectionFactoryId = Qualifiers.resolveName(key);
    }
  }

}

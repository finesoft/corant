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
package org.corant.suites.concurrency;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.Config;
import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午7:56:44
 *
 */
@ConfigKeyRoot(value = "concurrent.executor", ignoreNoAnnotatedItem = false, keyIndex = 2)
public class ManagedExecutorConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = -1732163277606881747L;

  private boolean longRunningTasks;
  private long hungTaskThreshold;
  private int corePoolSize;
  private int maxPoolSize;
  private Duration keepAliveTime;
  private Duration threadLifeTime;
  private RejectPolicy rejectPolicy;
  private int threadPriority;
  private String threadName;
  private int queueCapacity = Integer.MAX_VALUE;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ManagedExecutorConfig other = (ManagedExecutorConfig) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the corePoolSize
   */
  public int getCorePoolSize() {
    return max(corePoolSize, Systems.getCPUs());
  }

  /**
   *
   * @return the hungTaskThreshold
   */
  public long getHungTaskThreshold() {
    return hungTaskThreshold <= 0 ? 60000L : hungTaskThreshold;
  }

  /**
   *
   * @return the keepAliveTime
   */
  public Duration getKeepAliveTime() {
    return defaultObject(keepAliveTime, () -> Duration.ofSeconds(5));
  }

  /**
   *
   * @return the maxPoolSize
   */
  public int getMaxPoolSize() {
    return maxPoolSize <= getCorePoolSize() ? getCorePoolSize() : maxPoolSize;
  }

  /**
   *
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   * @return the queueCapacity
   */
  public int getQueueCapacity() {
    return queueCapacity < 0 ? Integer.MAX_VALUE : queueCapacity;
  }

  /**
   *
   * @return the rejectPolicy
   */
  public RejectPolicy getRejectPolicy() {
    return defaultObject(rejectPolicy, () -> RejectPolicy.ABORT);
  }

  /**
   *
   * @return the threadLifeTime
   */
  public Duration getThreadLifeTime() {
    return threadLifeTime;
  }

  /**
   *
   * @return the threadName
   */
  public String getThreadName() {
    return defaultObject(threadName, () -> name + "-thread");
  }

  /**
   *
   * @return the threadPriority
   */
  public int getThreadPriority() {
    return threadPriority <= 0 ? Thread.NORM_PRIORITY : threadPriority;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

  /**
   *
   * @return the longRunningTasks
   */
  public boolean isLongRunningTasks() {
    return longRunningTasks;
  }

  @Override
  public boolean isValid() {
    return isNotBlank(getName());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }

  /**
   *
   * @param corePoolSize the corePoolSize to set
   */
  protected void setCorePoolSize(int corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  /**
   *
   * @param hungTaskThreshold the hungTaskThreshold to set
   */
  protected void setHungTaskThreshold(long hungTaskThreshold) {
    this.hungTaskThreshold = hungTaskThreshold;
  }

  /**
   *
   * @param keepAliveTime the keepAliveTime to set
   */
  protected void setKeepAliveTime(Duration keepAliveTime) {
    this.keepAliveTime = keepAliveTime;
  }

  /**
   *
   * @param longRunningTasks the longRunningTasks to set
   */
  protected void setLongRunningTasks(boolean longRunningTasks) {
    this.longRunningTasks = longRunningTasks;
  }

  /**
   *
   * @param maxPoolSize the maxPoolSize to set
   */
  protected void setMaxPoolSize(int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  /**
   *
   * @param name the name to set
   */
  @Override
  protected void setName(String name) {
    this.name = defaultTrim(name);
  }

  /**
   *
   * @param queueCapacity the queueCapacity to set
   */
  protected void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  /**
   *
   * @param rejectPolicy the rejectPolicy to set
   */
  protected void setRejectPolicy(RejectPolicy rejectPolicy) {
    this.rejectPolicy = rejectPolicy;
  }

  /**
   *
   * @param threadLifeTime the threadLifeTime to set
   */
  protected void setThreadLifeTime(Duration threadLifeTime) {
    this.threadLifeTime = threadLifeTime;
  }

  /**
   *
   * @param threadName the threadName to set
   */
  protected void setThreadName(String threadName) {
    this.threadName = threadName;
  }

  /**
   *
   * @param threadPriority the threadPriority to set
   */
  protected void setThreadPriority(int threadPriority) {
    this.threadPriority = threadPriority;
  }

}

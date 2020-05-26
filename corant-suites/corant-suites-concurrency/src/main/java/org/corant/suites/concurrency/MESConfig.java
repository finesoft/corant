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

import static org.corant.shared.util.StringUtils.defaultTrim;
import java.time.Duration;
import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午7:56:44
 *
 */
public class MESConfig {

  private String name;
  private boolean longRunningTasks;
  private long hungTaskThreshold;
  private int corePoolSize;
  private int maxPoolSize;
  private Duration keepAliveTime;
  private Duration threadLifeTime;
  private RejectPolicy rejectPolicy;
  private int threadPriority;
  private String threadName;
  private int queueCapacity;
  private boolean fair;

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
    MESConfig other = (MESConfig) obj;
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
    return corePoolSize;
  }

  /**
   *
   * @return the hungTaskThreshold
   */
  public long getHungTaskThreshold() {
    return hungTaskThreshold;
  }

  /**
   *
   * @return the keepAliveTime
   */
  public Duration getKeepAliveTime() {
    return keepAliveTime;
  }

  /**
   *
   * @return the maxPoolSize
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   *
   * @return the queueCapacity
   */
  public int getQueueCapacity() {
    return queueCapacity;
  }

  /**
   *
   * @return the rejectPolicy
   */
  public RejectPolicy getRejectPolicy() {
    return rejectPolicy;
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
    return threadName;
  }

  /**
   *
   * @return the threadPriority
   */
  public int getThreadPriority() {
    return threadPriority;
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
   * @return the fair
   */
  public boolean isFair() {
    return fair;
  }

  /**
   *
   * @return the longRunningTasks
   */
  public boolean isLongRunningTasks() {
    return longRunningTasks;
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
   * @param fair the fair to set
   */
  protected void setFair(boolean fair) {
    this.fair = fair;
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

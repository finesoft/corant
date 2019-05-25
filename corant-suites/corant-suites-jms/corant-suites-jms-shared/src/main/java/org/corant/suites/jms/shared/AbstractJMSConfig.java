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
package org.corant.suites.jms.shared;

import static org.corant.shared.util.ObjectUtils.max;
import static org.corant.shared.util.StringUtils.defaultTrim;
import org.corant.shared.util.StringUtils;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 上午10:30:53
 *
 */
public abstract class AbstractJMSConfig {

  public static final String JMS_ENABLE = ".enable";
  public static final String JMS_XA = ".xa";
  public static final String JMS_REC_TSK_INIT_DELAYMS = ".receiver-task-initial-delayMs";
  public static final String JMS_REC_TSK_DELAYMS = ".receiver-task-delayMs";
  public static final String JMS_REC_TSK_THREADS = ".receiver-task-threads";

  public final static AbstractJMSConfig DFLT_INSTANCE = new AbstractJMSConfig() {};
  // the connection factory id means a artemis server or cluster
  private String connectionFactoryId = StringUtils.EMPTY;
  private boolean enable = true;
  private boolean xa = true;
  private long receiveTaskInitialDelayMs = 0;
  private long receiveTaskDelayMs = 1000L;
  private int receiveTaskThreads = max(2, Runtime.getRuntime().availableProcessors());

  /**
   *
   * @return the connectionFactoryId
   */
  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  /**
   *
   * @return the receiveTaskDelayMs
   */
  public long getReceiveTaskDelayMs() {
    return receiveTaskDelayMs;
  }

  /**
   *
   * @return the receiveTaskInitialDelayMs
   */
  public long getReceiveTaskInitialDelayMs() {
    return receiveTaskInitialDelayMs;
  }

  /**
   *
   * @return the receiveTaskThreads
   */
  public int getReceiveTaskThreads() {
    return receiveTaskThreads;
  }

  /**
   *
   * @return the enable
   */
  public boolean isEnable() {
    return enable;
  }

  /**
   *
   * @return the xa
   */
  public boolean isXa() {
    return xa;
  }

  /**
   *
   * @param connectionFactoryId the connectionFactoryId to set
   */
  protected void setConnectionFactoryId(String connectionFactoryId) {
    this.connectionFactoryId = defaultTrim(connectionFactoryId);
  }

  /**
   *
   * @param enable the enable to set
   */
  protected void setEnable(boolean enable) {
    this.enable = enable;
  }

  /**
   *
   * @param receiveTaskDelayMs the receiveTaskDelayMs to set
   */
  protected void setReceiveTaskDelayMs(long receiveTaskDelayMs) {
    this.receiveTaskDelayMs = receiveTaskDelayMs;
  }

  /**
   *
   * @param receiveTaskInitialDelayMs the receiveTaskInitialDelayMs to set
   */
  protected void setReceiveTaskInitialDelayMs(long receiveTaskInitialDelayMs) {
    this.receiveTaskInitialDelayMs = receiveTaskInitialDelayMs;
  }

  /**
   *
   * @param receiveTaskThreads the receiveTaskThreads to set
   */
  protected void setReceiveTaskThreads(int receiveTaskThreads) {
    this.receiveTaskThreads = receiveTaskThreads;
  }

  /**
   *
   * @param xa the xa to set
   */
  protected void setXa(boolean xa) {
    this.xa = xa;
  }

}

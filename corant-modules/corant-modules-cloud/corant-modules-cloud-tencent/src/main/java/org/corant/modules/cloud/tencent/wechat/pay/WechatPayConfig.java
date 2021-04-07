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
package org.corant.modules.cloud.tencent.wechat.pay;

import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;

/**
 * corant-modules-cloud-tencent
 *
 * @author bingo 上午10:00:26
 *
 */
@ConfigKeyRoot(value = "corant.cloud.tencent.wechat.pay", keyIndex = 5,
    ignoreNoAnnotatedItem = false)
public class WechatPayConfig implements DeclarativeConfig {

  private static final long serialVersionUID = 7248857141312758050L;

  protected String appId;

  protected String mchId;

  protected String key;

  protected String certUri;

  protected int httpConnectTimeoutMs;

  protected int httpReadTimeoutMs;

  protected boolean shouldAutoReport;

  protected int reportWorkerNum;

  protected int reportQueueMaxSize;

  protected int reportBatchSize;

  /**
   * 
   * @return the appId
   */
  public String getAppId() {
    return appId;
  }

  /**
   * 
   * @return the certUri
   */
  public String getCertUri() {
    return certUri;
  }

  /**
   * 
   * @return the httpConnectTimeoutMs
   */
  public int getHttpConnectTimeoutMs() {
    return httpConnectTimeoutMs;
  }

  /**
   * 
   * @return the httpReadTimeoutMs
   */
  public int getHttpReadTimeoutMs() {
    return httpReadTimeoutMs;
  }

  /**
   * 
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * 
   * @return the mchId
   */
  public String getMchId() {
    return mchId;
  }

  /**
   * 
   * @return the reportBatchSize
   */
  public int getReportBatchSize() {
    return reportBatchSize;
  }

  /**
   * 
   * @return the reportQueueMaxSize
   */
  public int getReportQueueMaxSize() {
    return reportQueueMaxSize;
  }

  /**
   * 
   * @return the reportWorkerNum
   */
  public int getReportWorkerNum() {
    return reportWorkerNum;
  }

  /**
   * 
   * @return the shouldAutoReport
   */
  public boolean isShouldAutoReport() {
    return shouldAutoReport;
  }

}

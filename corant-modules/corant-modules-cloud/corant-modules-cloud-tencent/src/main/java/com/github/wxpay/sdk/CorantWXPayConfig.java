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
package com.github.wxpay.sdk;

import static org.corant.context.Beans.find;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources;

/**
 * corant-modules-cloud-tencent
 *
 * @author bingo 上午10:00:26
 *
 */
@ConfigKeyRoot(value = "corant.cloud.tencent.wechat.pay", keyIndex = 5,
    ignoreNoAnnotatedItem = false)
public class CorantWXPayConfig extends WXPayConfig implements DeclarativeConfig {

  private static final long serialVersionUID = 7248857141312758050L;

  public static final IWXPayDomain DEFAULT_IWXPAYDOMAIN = new IWXPayDomain() {
    @Override
    public DomainInfo getDomain(WXPayConfig config) {
      return new DomainInfo(WXPayConstants.DOMAIN_API, true);
    }

    @Override
    public void report(String domain, long elapsedTimeMillis, Exception ex) {}
  };

  @ConfigKeyItem(defaultValue = "false")
  protected boolean useSandbox;

  protected String notifyUrl;

  protected String refundNotifyUrl;

  protected String appId;

  protected String mchId;

  protected String key;

  protected String certUri;

  protected int httpConnectTimeoutMs;

  protected int httpReadTimeoutMs;

  protected boolean autoReport;

  protected int reportWorkerNum;

  protected int reportQueueMaxSize;

  protected int reportBatchSize;

  @Override
  public int getHttpConnectTimeoutMs() {
    return httpConnectTimeoutMs;
  }

  @Override
  public int getHttpReadTimeoutMs() {
    return httpReadTimeoutMs;
  }

  public String getNotifyUrl() {
    return notifyUrl;
  }

  public String getRefundNotifyUrl() {
    return refundNotifyUrl;
  }

  @Override
  public int getReportBatchSize() {
    return reportBatchSize;
  }

  @Override
  public int getReportQueueMaxSize() {
    return reportQueueMaxSize;
  }

  @Override
  public int getReportWorkerNum() {
    return reportWorkerNum;
  }

  public boolean isUseSandbox() {
    return useSandbox;
  }

  @Override
  public boolean shouldAutoReport() {
    return autoReport;
  }

  @Override
  String getAppID() {
    return appId;
  }

  @Override
  InputStream getCertStream() {
    try {
      if (isNotBlank(certUri)) {
        return Resources.from(certUri).findFirst()
            .orElseThrow(
                () -> new CorantRuntimeException("Can't find cert resource from %s!", certUri))
            .openInputStream();
      }
      return null;
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  String getKey() {
    return key;
  }

  @Override
  String getMchID() {
    return mchId;
  }

  @Override
  IWXPayDomain getWXPayDomain() {
    return find(IWXPayDomain.class).orElse(DEFAULT_IWXPAYDOMAIN);
  }

}

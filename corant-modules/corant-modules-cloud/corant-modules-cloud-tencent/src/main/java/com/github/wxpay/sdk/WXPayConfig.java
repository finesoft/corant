/*
 * <p> All code base from this package(com.github.wxpay.sdk) are copy from <a href=
 * "https://pay.weixin.qq.com/wiki/doc/api/download/WxPayAPI_JAVA.zip">pay.weixin.qq.com</a>, the
 * original source code version is 3.0.9; all the source code below this
 * package(com.github.wxpay.sdk) are belongs to the original publisher, if there is infringement,
 * please inform me(finesoft@gmail.com).
 *
 * <b>Notices:</b> This package(com.github.wxpay.sdk) is only used for learning or reference, not in
 * the production environment; If you use the package in a production environment or redistribute
 * it, any problems arising therefrom are irrelevant to us, we do not assume any legal
 * responsibility.
 */
package com.github.wxpay.sdk;

import java.io.InputStream;

public abstract class WXPayConfig {

  /**
   * HTTP(S) 连接超时时间，单位毫秒
   *
   * @return
   */
  public int getHttpConnectTimeoutMs() {
    return 6 * 1000;
  }

  /**
   * HTTP(S) 读数据超时时间，单位毫秒
   *
   * @return
   */
  public int getHttpReadTimeoutMs() {
    return 8 * 1000;
  }

  /**
   * 批量上报，一次最多上报多个数据
   *
   * @return
   */
  public int getReportBatchSize() {
    return 10;
  }

  /**
   * 健康上报缓存消息的最大数量。会有线程去独立上报 粗略计算：加入一条消息200B，10000消息占用空间 2000 KB，约为2MB，可以接受
   *
   * @return
   */
  public int getReportQueueMaxSize() {
    return 10000;
  }

  /**
   * 进行健康上报的线程的数量
   *
   * @return
   */
  public int getReportWorkerNum() {
    return 6;
  }

  /**
   * 是否自动上报。 若要关闭自动上报，子类中实现该函数返回 false 即可。
   *
   * @return
   */
  public boolean shouldAutoReport() {
    return true;
  }

  /**
   * 获取 App ID
   *
   * @return App ID
   */
  abstract String getAppID();

  /**
   * 获取商户证书内容
   *
   * @return 商户证书内容
   */
  abstract InputStream getCertStream();

  /**
   * 获取 API 密钥
   *
   * @return API密钥
   */
  abstract String getKey();

  /**
   * 获取 Mch ID
   *
   * @return Mch ID
   */
  abstract String getMchID();

  /**
   * 获取WXPayDomain, 用于多域名容灾自动切换
   *
   * @return
   */
  abstract IWXPayDomain getWXPayDomain();

}

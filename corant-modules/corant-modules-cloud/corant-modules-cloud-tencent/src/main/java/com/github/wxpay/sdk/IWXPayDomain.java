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

/**
 * 域名管理，实现主备域名自动切换
 */
public interface IWXPayDomain {
  /**
   * 获取域名
   *
   * @param config 配置
   * @return 域名
   */
  DomainInfo getDomain(final WXPayConfig config);

  /**
   * 上报域名网络状况
   *
   * @param domain 域名。 比如：api.mch.weixin.qq.com
   * @param elapsedTimeMillis 耗时
   * @param ex 网络请求中出现的异常。 null表示没有异常 ConnectTimeoutException，表示建立网络连接异常 UnknownHostException，
   *        表示dns解析异常
   */
  void report(final String domain, long elapsedTimeMillis, final Exception ex);

  static class DomainInfo {
    public String domain; // 域名
    public boolean primaryDomain; // 该域名是否为主域名。例如:api.mch.weixin.qq.com为主域名

    public DomainInfo(String domain, boolean primaryDomain) {
      this.domain = domain;
      this.primaryDomain = primaryDomain;
    }

    @Override
    public String toString() {
      return "DomainInfo{" + "domain='" + domain + '\'' + ", primaryDomain=" + primaryDomain + '}';
    }
  }

}

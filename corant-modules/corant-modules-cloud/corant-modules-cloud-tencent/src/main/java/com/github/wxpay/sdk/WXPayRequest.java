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

import static com.github.wxpay.sdk.WXPayConstants.USER_AGENT;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class WXPayRequest {
  private WXPayConfig config;

  public WXPayRequest(WXPayConfig config) throws Exception {

    this.config = config;
  }

  /**
   * 可重试的，双向认证的请求
   *
   * @param urlSuffix
   * @param uuid
   * @param data
   * @return
   */
  public String requestWithCert(String urlSuffix, String uuid, String data, boolean autoReport)
      throws Exception {
    return request(urlSuffix, uuid, data, config.getHttpConnectTimeoutMs(),
        config.getHttpReadTimeoutMs(), true, autoReport);
  }

  /**
   * 可重试的，双向认证的请求
   *
   * @param urlSuffix
   * @param uuid
   * @param data
   * @param connectTimeoutMs
   * @param readTimeoutMs
   * @return
   */
  public String requestWithCert(String urlSuffix, String uuid, String data, int connectTimeoutMs,
      int readTimeoutMs, boolean autoReport) throws Exception {
    return request(urlSuffix, uuid, data, connectTimeoutMs, readTimeoutMs, true, autoReport);
  }

  /**
   * 可重试的，非双向认证的请求
   *
   * @param urlSuffix
   * @param uuid
   * @param data
   * @return
   */
  public String requestWithoutCert(String urlSuffix, String uuid, String data, boolean autoReport)
      throws Exception {
    return request(urlSuffix, uuid, data, config.getHttpConnectTimeoutMs(),
        config.getHttpReadTimeoutMs(), false, autoReport);
  }

  /**
   * 可重试的，非双向认证的请求
   *
   * @param urlSuffix
   * @param uuid
   * @param data
   * @param connectTimeoutMs
   * @param readTimeoutMs
   * @return
   */
  public String requestWithoutCert(String urlSuffix, String uuid, String data, int connectTimeoutMs,
      int readTimeoutMs, boolean autoReport) throws Exception {
    return request(urlSuffix, uuid, data, connectTimeoutMs, readTimeoutMs, false, autoReport);
  }

  private String request(String urlSuffix, String uuid, String data, int connectTimeoutMs,
      int readTimeoutMs, boolean useCert, boolean autoReport) throws Exception {
    Exception exception = null;
    long elapsedTimeMillis = 0;
    long startTimestampMs = WXPayUtil.getCurrentTimestampMs();
    boolean firstHasDnsErr = false;
    boolean firstHasConnectTimeout = false;
    boolean firstHasReadTimeout = false;
    IWXPayDomain.DomainInfo domainInfo = config.getWXPayDomain().getDomain(config);
    if (domainInfo == null) {
      throw new Exception("WXPayConfig.getWXPayDomain().getDomain() is empty or null");
    }
    try {
      String result = requestOnce(domainInfo.domain, urlSuffix, uuid, data, connectTimeoutMs,
          readTimeoutMs, useCert);
      elapsedTimeMillis = WXPayUtil.getCurrentTimestampMs() - startTimestampMs;
      config.getWXPayDomain().report(domainInfo.domain, elapsedTimeMillis, null);
      WXPayReport.getInstance(config).report(uuid, elapsedTimeMillis, domainInfo.domain,
          domainInfo.primaryDomain, connectTimeoutMs, readTimeoutMs, firstHasDnsErr,
          firstHasConnectTimeout, firstHasReadTimeout);
      return result;
    } catch (UnknownHostException ex) { // dns 解析错误，或域名不存在
      exception = ex;
      firstHasDnsErr = true;
      elapsedTimeMillis = WXPayUtil.getCurrentTimestampMs() - startTimestampMs;
      WXPayUtil.getLogger().warn("UnknownHostException for domainInfo {}", domainInfo);
      WXPayReport.getInstance(config).report(uuid, elapsedTimeMillis, domainInfo.domain,
          domainInfo.primaryDomain, connectTimeoutMs, readTimeoutMs, firstHasDnsErr,
          firstHasConnectTimeout, firstHasReadTimeout);
    } catch (ConnectTimeoutException ex) {
      exception = ex;
      firstHasConnectTimeout = true;
      elapsedTimeMillis = WXPayUtil.getCurrentTimestampMs() - startTimestampMs;
      WXPayUtil.getLogger().warn("connect timeout happened for domainInfo {}", domainInfo);
      WXPayReport.getInstance(config).report(uuid, elapsedTimeMillis, domainInfo.domain,
          domainInfo.primaryDomain, connectTimeoutMs, readTimeoutMs, firstHasDnsErr,
          firstHasConnectTimeout, firstHasReadTimeout);
    } catch (SocketTimeoutException ex) {
      exception = ex;
      firstHasReadTimeout = true;
      elapsedTimeMillis = WXPayUtil.getCurrentTimestampMs() - startTimestampMs;
      WXPayUtil.getLogger().warn("timeout happened for domainInfo {}", domainInfo);
      WXPayReport.getInstance(config).report(uuid, elapsedTimeMillis, domainInfo.domain,
          domainInfo.primaryDomain, connectTimeoutMs, readTimeoutMs, firstHasDnsErr,
          firstHasConnectTimeout, firstHasReadTimeout);
    } catch (Exception ex) {
      exception = ex;
      elapsedTimeMillis = WXPayUtil.getCurrentTimestampMs() - startTimestampMs;
      WXPayReport.getInstance(config).report(uuid, elapsedTimeMillis, domainInfo.domain,
          domainInfo.primaryDomain, connectTimeoutMs, readTimeoutMs, firstHasDnsErr,
          firstHasConnectTimeout, firstHasReadTimeout);
    }
    config.getWXPayDomain().report(domainInfo.domain, elapsedTimeMillis, exception);
    throw exception;
  }

  /**
   * 请求，只请求一次，不做重试
   *
   * @param domain
   * @param urlSuffix
   * @param uuid
   * @param data
   * @param connectTimeoutMs
   * @param readTimeoutMs
   * @param useCert 是否使用证书，针对退款、撤销等操作
   * @return
   * @throws Exception
   */
  private String requestOnce(final String domain, String urlSuffix, String uuid, String data,
      int connectTimeoutMs, int readTimeoutMs, boolean useCert) throws Exception {
    BasicHttpClientConnectionManager connManager;
    if (useCert) {
      // 证书
      char[] password = config.getMchID().toCharArray();
      InputStream certStream = config.getCertStream();
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(certStream, password);

      // 实例化密钥库 & 初始化密钥工厂
      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, password);

      // 创建 SSLContext
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

      SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
          sslContext, new String[] {"TLSv1"}, null, new DefaultHostnameVerifier());

      connManager =
          new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .register("https", sslConnectionSocketFactory).build(), null, null, null);
    } else {
      connManager = new BasicHttpClientConnectionManager(
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .register("https", SSLConnectionSocketFactory.getSocketFactory()).build(),
          null, null, null);
    }

    HttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connManager).build();

    String url = "https://" + domain + urlSuffix;
    HttpPost httpPost = new HttpPost(url);

    RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(readTimeoutMs)
        .setConnectTimeout(connectTimeoutMs).build();
    httpPost.setConfig(requestConfig);

    StringEntity postEntity = new StringEntity(data, "UTF-8");
    httpPost.addHeader("Content-Type", "text/xml");
    httpPost.addHeader("User-Agent", USER_AGENT + " " + config.getMchID());
    httpPost.setEntity(postEntity);

    HttpResponse httpResponse = httpClient.execute(httpPost);
    HttpEntity httpEntity = httpResponse.getEntity();
    return EntityUtils.toString(httpEntity, "UTF-8");

  }
}

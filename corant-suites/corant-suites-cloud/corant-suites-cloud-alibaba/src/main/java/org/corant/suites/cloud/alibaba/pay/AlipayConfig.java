package org.corant.suites.cloud.alibaba.pay;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * corant <br>
 *
 * @author sushuaihao 2020/10/29
 * @since
 */
@ApplicationScoped
public class AlipayConfig {
  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.appid")
  private String appid;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.merchant-private-key")
  private String merchantPrivateKey;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.app-public-key-path")
  private String appPublicKeyPath;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.cert-public-key-path")
  private String alipayCertPublicKeyPath;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.notify-url")
  private String notifyUrl;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.root-cert-path")
  private String rootCertPath;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.sign-type", defaultValue = "RSA2")
  private String signType;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.charset", defaultValue = "utf-8")
  private String charset;

  @Inject
  @ConfigProperty(name = "cloud.alibaba.web.pay.gateway-url")
  private String gatewayUrl;

  public String getAlipayCertPublicKeyPath() {
    return alipayCertPublicKeyPath;
  }

  public String getAppPublicKeyPath() {
    return appPublicKeyPath;
  }

  public String getAppid() {
    return appid;
  }

  public String getCharset() {
    return charset;
  }

  public String getGatewayUrl() {
    return gatewayUrl;
  }

  public String getMerchantPrivateKey() {
    return merchantPrivateKey;
  }

  public String getNotifyUrl() {
    return notifyUrl;
  }

  public String getRootCertPath() {
    return rootCertPath;
  }

  public String getSignType() {
    return signType;
  }
}

package org.corant.modules.cloud.alibaba.pay;

import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-cloud-alibaba
 *
 * @author sushuaihao 2020/10/29
 * @since
 */
@ConfigKeyRoot(value = "corant.cloud.alibaba.pay", keyIndex = 4, ignoreNoAnnotatedItem = false)
public class AlipayConfig implements DeclarativeConfig {

  private static final long serialVersionUID = 6560494718072597103L;

  protected String appid;

  protected String merchantPrivateKey;

  protected String appPublicKeyPath;

  protected String certPublicKeyPath;

  protected String notifyUrl;

  protected String rootCertPath;

  @ConfigKeyItem(defaultValue = "RSA2")
  protected String signType;

  @ConfigKeyItem(defaultValue = "utf-8")
  protected String charset;

  protected String gatewayUrl;

  public String getAppid() {
    return appid;
  }

  public String getAppPublicKeyPath() {
    return appPublicKeyPath;
  }

  public String getCertPublicKeyPath() {
    return certPublicKeyPath;
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

  @Override
  public boolean isValid() {
    return DeclarativeConfig.super.isValid();
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    DeclarativeConfig.super.onPostConstruct(config, key);
  }

}

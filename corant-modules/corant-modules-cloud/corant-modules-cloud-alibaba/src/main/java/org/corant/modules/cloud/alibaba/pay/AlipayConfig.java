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

  protected String serverUrl;

  protected String appId;

  protected String privateKey;

  @ConfigKeyItem(defaultValue = "json")
  protected String format;

  @ConfigKeyItem(defaultValue = "utf-8")
  protected String charset;

  @ConfigKeyItem(defaultValue = "RSA2")
  protected String signType;

  protected String certPath;

  protected String alipayPublicCertPath;

  protected String rootCertPath;

  protected String encryptor;

  protected String encryptType;

  protected String proxyHost;

  protected int proxyPort;

  /**
   *
   * @return the alipayPublicCertPath
   */
  public String getAlipayPublicCertPath() {
    return alipayPublicCertPath;
  }

  /**
   *
   * @return the appId
   */
  public String getAppId() {
    return appId;
  }

  /**
   *
   * @return the certPath
   */
  public String getCertPath() {
    return certPath;
  }

  /**
   *
   * @return the charset
   */
  public String getCharset() {
    return charset;
  }

  /**
   *
   * @return the encryptor
   */
  public String getEncryptor() {
    return encryptor;
  }

  /**
   *
   * @return the encryptType
   */
  public String getEncryptType() {
    return encryptType;
  }

  /**
   *
   * @return the format
   */
  public String getFormat() {
    return format;
  }

  /**
   *
   * @return the privateKey
   */
  public String getPrivateKey() {
    return privateKey;
  }

  /**
   *
   * @return the proxyHost
   */
  public String getProxyHost() {
    return proxyHost;
  }

  /**
   *
   * @return the proxyPort
   */
  public int getProxyPort() {
    return proxyPort;
  }

  /**
   *
   * @return the rootCertPath
   */
  public String getRootCertPath() {
    return rootCertPath;
  }

  /**
   *
   * @return the serverUrl
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   *
   * @return the signType
   */
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

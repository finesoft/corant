package org.corant.modules.cloud.tencent.wechat.oauth;

import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;

/**
 * corant-modules-cloud-tencent
 *
 * @author sushuaihao 2020/9/11
 * @since
 */
@ConfigKeyRoot(value = "corant.cloud.tencent.wechat.oauth", keyIndex = 5,
    ignoreNoAnnotatedItem = false)
public class WechatOAuthConfig implements DeclarativeConfig {

  private static final long serialVersionUID = 4325579136120573042L;

  protected String appid;

  protected String secret;

  protected String redirectUri;

  protected String qrCodeUrl;

  /**
   * 应用唯一标识
   *
   * @return getAppid
   */
  public String getAppid() {
    return appid;
  }

  public String getQrCodeUrl() {
    return qrCodeUrl;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public String getSecret() {
    return secret;
  }
}

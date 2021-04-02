package org.corant.modules.cloud.tencent.wechat;

import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;

/**
 * corant-modules-cloud-tencent
 *
 * @author sushuaihao 2020/9/11
 * @since
 */
@ConfigKeyRoot(value = "corant.cloud.tencent.wechat", keyIndex = 4, ignoreNoAnnotatedItem = false)
public class WechatOAuthConfig implements DeclarativeConfig {

  private static final long serialVersionUID = 4325579136120573042L;

  private String appid;

  private String secret;

  private String redirectUri;

  private String qrCodeUrl;

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

package org.corant.suites.cloud.tencent.wechat;

import org.corant.config.declarative.ConfigKeyRoot;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/9/11
 * @since
 */
@ApplicationScoped
public class WechatOAuthConfig {

  @Inject
  @ConfigProperty(name = "cloud.tencent.wechat.appid")
  private String appid;

  @Inject
  @ConfigProperty(name = "cloud.tencent.wechat.secret")
  private String secret;

  @Inject
  @ConfigProperty(name = "cloud.tencent.wechat.redirect-uri")
  private String redirectUri;

  @Inject
  @ConfigProperty(name = "cloud.tencent.wechat.qr-code-url")
  private String qrCodeUrl;

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

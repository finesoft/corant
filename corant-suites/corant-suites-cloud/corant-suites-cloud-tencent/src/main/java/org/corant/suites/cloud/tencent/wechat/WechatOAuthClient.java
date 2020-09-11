package org.corant.suites.cloud.tencent.wechat;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * corant <br>
 *
 * @see <a href=
 *      "https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html">微信登录功能</a>
 * @auther sushuaihao 2020/9/11
 * @since
 */
@Path("/sns")
@RegisterRestClient(baseUri = "https://api.weixin.qq.com/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public interface WechatOAuthClient {

  /**
   * 检验授权凭证（access_token）是否有效
   *
   * @see <a href=
   *      "https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Authorized_Interface_Calling_UnionID.html">
   *      检验授权凭证（access_token）是否有效</a>
   * @param token
   * @param openId
   * @return authToken
   */
  @GET
  @Path("/auth")
  Map<?, ?> authToken(@QueryParam("access_token") String token,
      @QueryParam("openid") String openId);

  /**
   * 通过code获取access_token
   *
   * @see <a href=
   *      "https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Authorized_Interface_Calling_UnionID.html">
   *      通过code获取access_token</a>
   * @param appid 应用唯一标识，在微信开放平台提交应用审核通过后获得
   * @param secret 应用密钥AppSecret，在微信开放平台提交应用审核通过后获得
   * @param code 获取的code参数
   * @param grantType authorization_code
   * @return
   */
  @GET
  @Path("/oauth2/access_token")
  Map<?, ?> grantAccessToken(@QueryParam("appid") String appid, @QueryParam("secret") String secret,
      @QueryParam("code") String code, @QueryParam("grant_type") String grantType);

  /**
   * 刷新或续期access_token使用
   *
   * @see <a href=
   *      "https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Authorized_Interface_Calling_UnionID.html">
   *      刷新或续期access_token使用</a>
   * @param appid 应用唯一标识
   * @param grantType refresh_token
   * @param refreshToken 填写通过access_token获取到的refresh_token参数
   * @return
   */
  @GET
  @Path("/oauth2/refresh_token")
  Map<?, ?> refreshToken(@QueryParam("appid") String appid,
      @QueryParam("grant_type") String grantType, @QueryParam("refresh_token") String refreshToken);

  /**
   * 获取用户个人信息（UnionID机制）
   *
   * @see <a href=
   *      "https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Authorized_Interface_Calling_UnionID.html">
   *      获取用户个人信息（UnionID机制）</a>
   *
   * @param token 调用凭证
   * @param openId 普通用户的标识，对当前开发者帐号唯一
   * @param lang 国家地区语言版本，zh_CN 简体，zh_TW 繁体，en 英语，默认为zh-CN
   * @return userInfo
   */
  @GET
  @Path("/userinfo")
  Map<?, ?> userInfo(@QueryParam("access_token") String token, @QueryParam("openid") String openId,
      @QueryParam("lang") String lang);
}

package org.corant.suites.cloud.tencent.wechat;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.*;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/9/11
 * @since
 */
@Path("/sns")
@RegisterRestClient(baseUri = "https://api.weixin.qq.com/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public interface WechatOAuthClient {
  @GET
  @Path("/auth")
  Map<?, ?> authToken(
      @QueryParam("access_token") String token, @QueryParam("openid") String openId);

  /**
   * @param appid
   * @param secret
   * @param code
   * @param grantType authorization_code
   * @return
   */
  @GET
  @Path("/oauth2/access_token")
  Map<?, ?> grantAccessToken(
      @QueryParam("appid") String appid,
      @QueryParam("secret") String secret,
      @QueryParam("code") String code,
      @QueryParam("grant_type") String grantType);

  /**
   * @param appid
   * @param grantType refresh_token
   * @param refreshToken
   * @return
   */
  @GET
  @Path("/oauth2/refresh_token")
  Map<?, ?> refreshToken(
      @QueryParam("appid") String appid,
      @QueryParam("grant_type") String grantType,
      @QueryParam("refresh_token") String refreshToken);

  @GET
  @Path("/userinfo")
  Map<?, ?> userInfo(@QueryParam("access_token") String token, @QueryParam("openid") String openId);
}

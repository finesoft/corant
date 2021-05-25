/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.devops.test.unit.web;

import static org.corant.shared.util.Maps.mapOf;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.devops.test.unit.web.RandomWebServerPortSourceProvider.RandomWebServerPortConfigSource;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.shared.normal.Defaults;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午7:37:08
 *
 */
@ApplicationScoped
public class RestAssuredInitializer {

  public static final String TEST_JWT = "corant.test.jwt";

  @Inject
  @ConfigProperty(name = RandomWebServerPortConfigSource.WEB_SERVER_PORT_PN, defaultValue = "0")
  private int port;

  @Inject
  @ConfigProperty(name = TEST_JWT)
  private Optional<String> jwt;

  @Inject
  @Any
  private Instance<AuthenticationScheme> authSchema;

  public static void initializeMultipartFormParam(RequestSpecification rs,
      Map<String, Object> params) {
    params.forEach((k, v) -> rs.multiPart(new IdiotMultiPartSpecification(k, v)));
  }

  public static RequestSpecification multipartOf(Map<String, Object> params) {
    RequestSpecification rs = RestAssured.given()
        .contentType("multipart/form-data; charset=" + Defaults.DFLT_CHARSET_STR);
    params.putIfAbsent("_charset_", Defaults.DFLT_CHARSET_STR);
    initializeMultipartFormParam(rs, params);
    return rs;
  }

  void initializeRestAssured(@Observes PostCorantReadyEvent event) {
    RestAssured.port = port;
    if (jwt.isPresent()) {
      PreemptiveOAuth2HeaderScheme auth = new PreemptiveOAuth2HeaderScheme();
      auth.setAccessToken(jwt.get());
      RestAssured.authentication = auth;
    } else if (authSchema.isResolvable()) {
      RestAssured.authentication = authSchema.get();
    }
  }

  /**
   * corant-devops-test-unit
   *
   * @author bingo 下午8:56:53
   *
   */
  public static final class IdiotMultiPartSpecification implements MultiPartSpecification {
    private final String k;
    private final Object v;

    /**
     * @param k
     * @param v
     */
    public IdiotMultiPartSpecification(String k, Object v) {
      this.k = k;
      this.v = v;
    }

    @Override
    public String getCharset() {
      return Defaults.DFLT_CHARSET_STR;
    }

    @Override
    public Object getContent() {
      return v;
    }

    @Override
    public String getControlName() {
      return k;
    }

    @Override
    public String getFileName() {
      return v instanceof File ? ((File) v).getName() : null;
    }

    @Override
    public Map<String, String> getHeaders() {
      return mapOf("Content-Disposition", new ContentDisposition("form-data", getControlName(),
          getFileName(), Defaults.DFLT_CHARSET, null, ZonedDateTime.now(), null, null).toString());
      // return v instanceof File
      // ? mapOf("Content-Disposition",
      // "form-data; name=\"" + getControlName() + "\"; filename=\"" + getFileName() + "\"",
      // "Content-Type", "application/octet-stream", "Content-Transfer-Encoding", "binary")
      // : mapOf("Content-Disposition", "form-data; name=\"" + k + "\"", "Content-Type",
      // "text/plain; charset=UTF-8", "Content-Transfer-Encoding", "8bit");
    }

    @Override
    public String getMimeType() {
      return v instanceof File ? "application/octet-stream" : "text/plain";
    }

    @Override
    public boolean hasFileName() {
      return v instanceof File;
    }
  }
}

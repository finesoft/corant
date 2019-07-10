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

import static org.corant.shared.util.MapUtils.mapOf;
import java.io.File;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.devops.test.unit.web.RandomWebServerPortSourceProvider.RandomWebServerPortConfigSource;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.restassured.RestAssured;
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

  @Inject
  @ConfigProperty(name = RandomWebServerPortConfigSource.WEB_SERVER_PORT_PN, defaultValue = "0")
  private int port;

  public static void initializeMultipartFormParam(RequestSpecification rs,
      Map<String, Object> params) {
    params.forEach((k, v) -> rs.multiPart(new IdiotMultiPartSpecification(k, v)));
  }

  public static RequestSpecification multipartOf(Map<String, Object> params) {
    RequestSpecification rs = RestAssured.given().contentType("multipart/form-data");
    initializeMultipartFormParam(rs, params);
    return rs;
  }

  void initializeRestAssured(@Observes PostCorantReadyEvent event) {
    RestAssured.port = port;
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
      return "utf-8";
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
      return v instanceof File
          ? mapOf("Content-Disposition",
              "form-data; name=\"" + getControlName() + "\"; filename=\"" + getFileName() + "\"",
              "Content-Type", "application/octet-stream", "Content-Transfer-Encoding", "binary")
          : mapOf("Content-Disposition", "form-data; name=\"" + k + "\"", "Content-Type",
              "text/plain; charset=UTF-8", "Content-Transfer-Encoding", "8bit");
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

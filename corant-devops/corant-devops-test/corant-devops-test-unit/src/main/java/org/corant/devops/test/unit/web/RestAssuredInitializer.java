/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.devops.test.unit.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.devops.test.unit.web.RandomWebServerPortSourceProvider.RandomWebServerPortConfigSource;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.restassured.RestAssured;

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

  void initializeRestAssured(@Observes PostCorantReadyEvent event) {
    RestAssured.port = port;
  }

}

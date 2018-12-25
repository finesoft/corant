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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.corant.devops.test.unit.CorantJunit4Runner;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午7:13:27
 *
 */
public class RandomWebServerPortSourceProvider implements ConfigSourceProvider {

  public final RandomWebServerPortConfigSource TEST_WSPORT_CFGSRC =
      new RandomWebServerPortConfigSource(RandomWebServerPortProducer.getServerPort());

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
    return Collections.singleton(TEST_WSPORT_CFGSRC);
  }

  public static class RandomWebServerPortConfigSource implements ConfigSource {

    public static final String WEB_SERVER_PORT_PN = "webserver.port";
    final Map<String, String> properties = new HashMap<>();
    final int port;

    public RandomWebServerPortConfigSource(final int port) {
      this.port = port;
      properties.put(WEB_SERVER_PORT_PN, port + "");
    }

    @Override
    public String getName() {
      return RandomWebServerPortConfigSource.class.getName();
    }

    @Override
    public int getOrdinal() {
      if (CorantJunit4Runner.enableRdmWebPorts.get()) {
        return Integer.MAX_VALUE;
      } else {
        return Integer.MIN_VALUE;
      }
    }

    @Override
    public Map<String, String> getProperties() {
      return properties;
    }

    @Override
    public String getValue(String propertyName) {
      if (propertyName.equals(WEB_SERVER_PORT_PN)) {
        return properties.getOrDefault(WEB_SERVER_PORT_PN, "0");
      }
      return null;
    }

  }
}

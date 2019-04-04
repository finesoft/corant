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
package org.corant.suites.jms.artemis.option4;

import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 上午10:08:11
 *
 */
public class ArtemisConfig {

  @Inject
  @ConfigProperty(name = "jms.artemis.username", defaultValue = "")
  private String userame;

  @Inject
  @ConfigProperty(name = "jms.artemis.password", defaultValue = "")
  private String password;

  @Inject
  @ConfigProperty(name = "jms.artemis.url", defaultValue = "")
  private String url;

  @Inject
  @ConfigProperty(name = "jms.artemis.host", defaultValue = "")
  private String host;

  @Inject
  @ConfigProperty(name = "jms.artemis.port", defaultValue = "")
  private int port;

  @Inject
  @ConfigProperty(name = "jms.artemis.connector-factory",
      defaultValue = "org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory")
  private String connectorFactory;

  @Inject
  @ConfigProperty(name = "jms.artemis.embedded-broker", defaultValue = "false")
  private boolean embeddedBroker;

  @Inject
  @ConfigProperty(name = "jms.artemis.ha", defaultValue = "false")
  private boolean ha;

  public String getConnectorFactory() {
    return connectorFactory;
  }

  public String getHost() {
    return host;
  }

  public String getPassword() {
    return password;
  }

  public Integer getPort() {
    return port;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return userame;
  }

  public boolean hasAuthentication() {
    return getUsername() != null && getPassword() != null;
  }

  public boolean isHa() {
    return ha;
  }

  public boolean startEmbeddedBroker() {
    return embeddedBroker;
  }
}

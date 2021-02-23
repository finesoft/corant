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
package org.corant.suites.mail;

import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.eclipse.microprofile.config.Config;

import javax.mail.Authenticator;
import java.util.Map;
import java.util.Properties;

import static org.corant.shared.util.Maps.toProperties;
import static org.corant.shared.util.Strings.isNoneBlank;

/**
 * corant-suites-mail
 *
 * @author bingo 下午12:01:55
 */
@ConfigKeyRoot("mail")
public class MailConfig implements DeclarativeConfig {

  private static final long serialVersionUID = -230483402482161319L;

  @ConfigKeyItem private String protocol;

  @ConfigKeyItem private String host;

  @ConfigKeyItem private int port;

  @ConfigKeyItem private String username;

  @ConfigKeyItem private String password;

  @ConfigKeyItem private int connectionTimeout;

  @ConfigKeyItem private Map<String, String> properties;

  private Properties mailProperties;

  private Authenticator authenticator;

  public MailConfig(
      String protocol,
      String host,
      int port,
      String username,
      String password,
      int connectionTimeout,
      Map<String, String> properties) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.connectionTimeout = connectionTimeout;
    this.properties = properties;
  }

  public Authenticator getAuthenticator() {
    return authenticator;
  }

  /** @return the connectionTimeout */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  /** @return the host */
  public String getHost() {
    return host;
  }

  public Properties getMailProperties() {
    return mailProperties;
  }

  /** @return the password */
  public String getPassword() {
    return password;
  }

  /** @return the port */
  public int getPort() {
    return port;
  }

  /** @return the properties */
  public Map<String, String> getProperties() {
    return properties;
  }

  /** @return the protocol */
  public String getProtocol() {
    return protocol;
  }

  /** @return the username */
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isValid() {
    return isNoneBlank(protocol, host);
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    mailProperties = toProperties(properties);
    authenticator = new DefaultAuthenticator(username, password);
  }
}

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
package org.corant.suites.webserver.undertow;

import static org.corant.shared.util.ObjectUtils.max;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-webserver-undertow
 *
 * @author bingo 下午3:08:00
 *
 */
@ApplicationScoped
public class UndertowWebServerConfig {

  @Inject
  @ConfigProperty(name = "webserver.undertow.io-threads")
  private Optional<Integer> ioThreads;

  @Inject
  @ConfigProperty(name = "webserver.undertow.not-request-timeout", defaultValue = "60000")
  private int notRequestTimeout;

  @Inject
  @ConfigProperty(name = "webserver.undertow.high-water", defaultValue = "1048576")
  private int highWater;

  @Inject
  @ConfigProperty(name = "webserver.undertow.low-water", defaultValue = "1048576")
  private int lowWater;

  @Inject
  @ConfigProperty(name = "webserver.undertow.tcp-nodelay", defaultValue = "true")
  private boolean tcpNoDelay;

  @Inject
  @ConfigProperty(name = "webserver.undertow.reuse-address", defaultValue = "true")
  private boolean reuseAddress;

  @Inject
  @ConfigProperty(name = "webserver.undertow.cork", defaultValue = "true")
  private boolean cork;

  @Inject
  @ConfigProperty(name = "webserver.undertow.enable-default-servlet", defaultValue = "false")
  private boolean enableDefaultServlet;

  @Inject
  @ConfigProperty(name = "webserver.undertow.buffer-size", defaultValue = "16364")
  private int bufferSize;

  @Inject
  @ConfigProperty(name = "webserver.undertow.eager-filter-init", defaultValue = "false")
  private boolean eagerFilterInit;

  @Inject
  @ConfigProperty(name = "webserver.undertow.enable-http2", defaultValue = "false")
  private boolean enableHttp2;

  @Inject
  @ConfigProperty(name = "webserver.undertow.enable-ajp", defaultValue = "false")
  private boolean enableAjp;

  @Inject
  @ConfigProperty(name = "webserver.undertow.balancing-tokens", defaultValue = "1")
  private int balancingTokens;

  @Inject
  @ConfigProperty(name = "webserver.undertow.balancing-connections", defaultValue = "2")
  private int balancingConnections;

  @Inject
  @ConfigProperty(name = "webserver.undertow.back-log", defaultValue = "1000")
  private int backLog;

  @Inject
  @ConfigProperty(name = "webserver.undertow.jsp-content-path")
  private Optional<String> jspContentPath;

  @Inject
  @ConfigProperty(name = "webserver.undertow.jsp-serving-path")
  private Optional<String> jspServingPath;

  @Inject
  @ConfigProperty(name = "webserver.undertow.static-content-path")
  private Optional<String> staticContentPath;

  @Inject
  @ConfigProperty(name = "webserver.undertow.static-serving-path")
  private Optional<String> staticServingPath;

  @Inject
  @ConfigProperty(name = "webserver.undertow.persistence-session", defaultValue = "false")
  private boolean persistenceSession;

  @Inject
  @ConfigProperty(name = "webserver.undertow.escape-error-message", defaultValue = "true")
  private boolean escapeErrorMessage;

  @Inject
  @ConfigProperty(name = "webserver.default-request-charset")
  private Optional<String> defaultRequestCharset;

  @Inject
  @ConfigProperty(name = "webserver.default-response-charset")
  private Optional<String> defaultResponseCharset;

  /**
   *
   * @return the backLog
   */
  public int getBackLog() {
    return backLog;
  }

  /**
   *
   * @return the balancingConnections
   */
  public int getBalancingConnections() {
    return balancingConnections;
  }

  /**
   *
   * @return the balancingTokens
   */
  public int getBalancingTokens() {
    return balancingTokens;
  }

  /**
   *
   * @return the bufferSize
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   *
   * @return the defaultRequestCharset
   */
  public Optional<String> getDefaultRequestCharset() {
    return defaultRequestCharset;
  }

  /**
   *
   * @return the defaultResponseCharset
   */
  public Optional<String> getDefaultResponseCharset() {
    return defaultResponseCharset;
  }

  /**
   *
   * @return the highWater
   */
  public int getHighWater() {
    return highWater;
  }

  /**
   *
   * @return the ioThreads
   */
  public Integer getIoThreads() {
    return ioThreads.orElse(max(Runtime.getRuntime().availableProcessors(), 2));
  }

  public Optional<String> getJspContentPath() {
    return jspContentPath;
  }

  /**
   *
   * @return the jspServingPath
   */
  public Optional<String> getJspServingPath() {
    return jspServingPath;
  }

  /**
   *
   * @return the lowWater
   */
  public int getLowWater() {
    return lowWater;
  }

  /**
   *
   * @return the notRequestTimeout
   */
  public int getNotRequestTimeout() {
    return notRequestTimeout;
  }

  /**
   *
   * @return the staticContentPaths
   */
  public Optional<String> getStaticContentPath() {
    return staticContentPath;
  }

  /**
   *
   * @return the staticServingPath
   */
  public Optional<String> getStaticServingPath() {
    return staticServingPath;
  }

  /**
   *
   * @return the cork
   */
  public boolean isCork() {
    return cork;
  }

  /**
   *
   * @return the eagerFilterInit
   */
  public boolean isEagerFilterInit() {
    return eagerFilterInit;
  }

  /**
   *
   * @return the enableAjp
   */
  public boolean isEnableAjp() {
    return enableAjp;
  }

  /**
   *
   * @return the enableDefaultServlet
   */
  public boolean isEnableDefaultServlet() {
    return enableDefaultServlet;
  }

  /**
   *
   * @return the enableHttp2
   */
  public boolean isEnableHttp2() {
    return enableHttp2;
  }

  /**
   *
   * @return the escapeErrorMessage
   */
  public boolean isEscapeErrorMessage() {
    return escapeErrorMessage;
  }

  /**
   *
   * @return isPersistenceSession
   */
  public boolean isPersistenceSession() {
    return persistenceSession;
  }

  /**
   *
   * @return the reuseAddress
   */
  public boolean isReuseAddress() {
    return reuseAddress;
  }

  /**
   *
   * @return the tcpNoDelay
   */
  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

}

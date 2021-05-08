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
package org.corant.modules.webserver.undertow;

import static org.corant.shared.util.Objects.max;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-webserver-undertow
 *
 * @author bingo 下午3:08:00
 *
 */
@ApplicationScoped
public class UndertowWebServerConfig {

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.io-threads")
  protected Optional<Integer> ioThreads;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.not-request-timeout", defaultValue = "60000")
  protected int notRequestTimeout;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.high-water", defaultValue = "1048576")
  protected int highWater;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.low-water", defaultValue = "1048576")
  protected int lowWater;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.tcp-nodelay", defaultValue = "true")
  protected boolean tcpNoDelay;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.reuse-address", defaultValue = "true")
  protected boolean reuseAddress;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.cork", defaultValue = "true")
  protected boolean cork;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.enable-default-servlet", defaultValue = "false")
  protected boolean enableDefaultServlet;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.buffer-size", defaultValue = "16364")
  protected int bufferSize; // 1024 * 16 -20, 20 some space for protocol headers

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.eager-filter-init", defaultValue = "false")
  protected boolean eagerFilterInit;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.enable-http2", defaultValue = "false")
  protected boolean enableHttp2;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.enable-ajp", defaultValue = "false")
  protected boolean enableAjp;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.balancing-tokens", defaultValue = "1")
  protected int balancingTokens;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.balancing-connections", defaultValue = "2")
  protected int balancingConnections;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.back-log", defaultValue = "1000")
  protected int backLog;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.jsp-content-path")
  protected Optional<String> jspContentPath;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.jsp-serving-path")
  protected Optional<String> jspServingPath;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.static-content-path")
  protected Optional<String> staticContentPath;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.static-serving-path")
  protected Optional<String> staticServingPath;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.static-paths")
  protected Optional<String> staticPaths;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.persistence-session", defaultValue = "false")
  protected boolean persistenceSession;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.default-session-timeout")
  protected Optional<Integer> defaultSessionTimeout;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.escape-error-message", defaultValue = "true")
  protected boolean escapeErrorMessage;

  @Inject
  @ConfigProperty(name = "corant.webserver.default-request-charset")
  protected Optional<String> defaultRequestCharset;

  @Inject
  @ConfigProperty(name = "corant.webserver.default-response-charset")
  protected Optional<String> defaultResponseCharset;

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

  public Optional<Integer> getDefaultSessionTimeout() {
    return defaultSessionTimeout;
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
    return ioThreads.orElse(max(Runtime.getRuntime().availableProcessors() * 2, 2));
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
   * @return the staticPaths
   */
  public Optional<String> getStaticPaths() {
    return staticPaths;
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

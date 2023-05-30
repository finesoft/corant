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
import java.util.Set;
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
  @ConfigProperty(name = "corant.webserver.undertow.enable-http2", defaultValue = "true")
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
  @ConfigProperty(name = "corant.webserver.undertow.welcome-pages",
      defaultValue = "index.html,index.htm,default.html,default.htm")
  protected Set<String> welcomePages;

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

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.websocket.buffer-pool.direct-buffer",
      defaultValue = "false")
  protected boolean websocketBufferPoolDirectBuffer;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.websocket.buffer-pool.buffer-size",
      defaultValue = "1024")
  protected Integer websocketBufferPoolBufferSize;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.websocket.buffer-pool.max-pool-size",
      defaultValue = "100")
  protected Integer websocketBufferPoolMaxPoolSize;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.websocket.buffer-pool.cache-size",
      defaultValue = "12")
  protected Integer websocketBufferPoolCacheSize;

  @Inject
  @ConfigProperty(name = "corant.webserver.undertow.websocket.buffer-pool.leak-decetion-percent",
      defaultValue = "0")
  protected Integer websocketBufferPoolLeakDecetionPercent;

  public int getBackLog() {
    return backLog;
  }

  public int getBalancingConnections() {
    return balancingConnections;
  }

  public int getBalancingTokens() {
    return balancingTokens;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public Optional<String> getDefaultRequestCharset() {
    return defaultRequestCharset;
  }

  public Optional<String> getDefaultResponseCharset() {
    return defaultResponseCharset;
  }

  public Optional<Integer> getDefaultSessionTimeout() {
    return defaultSessionTimeout;
  }

  public int getHighWater() {
    return highWater;
  }

  public Integer getIoThreads() {
    return ioThreads.orElse(max(Runtime.getRuntime().availableProcessors() * 2, 2));
  }

  public Optional<String> getJspContentPath() {
    return jspContentPath;
  }

  public Optional<String> getJspServingPath() {
    return jspServingPath;
  }

  public int getLowWater() {
    return lowWater;
  }

  public int getNotRequestTimeout() {
    return notRequestTimeout;
  }

  public Optional<String> getStaticContentPath() {
    return staticContentPath;
  }

  public Optional<String> getStaticPaths() {
    return staticPaths;
  }

  public Optional<String> getStaticServingPath() {
    return staticServingPath;
  }

  public Integer getWebsocketBufferPoolBufferSize() {
    return websocketBufferPoolBufferSize;
  }

  public Integer getWebsocketBufferPoolCacheSize() {
    return websocketBufferPoolCacheSize;
  }

  public Integer getWebsocketBufferPoolLeakDecetionPercent() {
    return websocketBufferPoolLeakDecetionPercent;
  }

  public Integer getWebsocketBufferPoolMaxPoolSize() {
    return websocketBufferPoolMaxPoolSize;
  }

  public Set<String> getWelcomePages() {
    return welcomePages;
  }

  public boolean isCork() {
    return cork;
  }

  public boolean isEagerFilterInit() {
    return eagerFilterInit;
  }

  public boolean isEnableAjp() {
    return enableAjp;
  }

  public boolean isEnableDefaultServlet() {
    return enableDefaultServlet;
  }

  public boolean isEnableHttp2() {
    return enableHttp2;
  }

  public boolean isEscapeErrorMessage() {
    return escapeErrorMessage;
  }

  public boolean isPersistenceSession() {
    return persistenceSession;
  }

  public boolean isReuseAddress() {
    return reuseAddress;
  }

  public boolean isTcpNoDelay() {
    return tcpNoDelay;
  }

  public boolean isWebsocketBufferPoolDirectBuffer() {
    return websocketBufferPoolDirectBuffer;
  }

}

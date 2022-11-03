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
package org.corant.modules.webserver.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.corant.modules.servlet.WebMetaDataProvider;
import org.corant.modules.servlet.metadata.WebFilterMetaData;
import org.corant.modules.servlet.metadata.WebListenerMetaData;
import org.corant.modules.servlet.metadata.WebServletMetaData;
import org.corant.modules.webserver.shared.WebServerHandlers.PostStartedHandler;
import org.corant.modules.webserver.shared.WebServerHandlers.PostStoppedHandler;
import org.corant.modules.webserver.shared.WebServerHandlers.PreStartHandler;
import org.corant.modules.webserver.shared.WebServerHandlers.PreStopHandler;
import org.corant.modules.websocket.WebSocketExtension;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-webserver-shared
 *
 * @author bingo 下午7:41:18
 *
 */
@ApplicationScoped
public abstract class AbstractWebServer implements WebServer {

  @Inject
  protected WebServerConfig config;

  @Inject
  @Any
  protected Instance<WebMetaDataProvider> metaDataProviders;

  @Inject
  @Any
  protected WebSocketExtension webSocketExtension;

  @Inject
  @Any
  protected Instance<PreStartHandler> preStartHandlers;

  @Inject
  @Any
  protected Instance<PostStartedHandler> postStartedHandlers;

  @Inject
  @Any
  protected Instance<PreStopHandler> preStopHandlers;

  @Inject
  @Any
  protected Instance<PostStoppedHandler> postStoppedHandlers;

  /**
   *
   * @return the postStartedHandlers
   */
  public Stream<PostStartedHandler> getPostStartedHandlers() {
    if (!postStartedHandlers.isUnsatisfied()) {
      return postStartedHandlers.stream().sorted(PostStartedHandler::compare);
    }
    return Stream.empty();
  }

  /**
   *
   * @return the postStoppedHandlers
   */
  public Stream<PostStoppedHandler> getPostStoppedHandlers() {
    if (!postStoppedHandlers.isUnsatisfied()) {
      return postStoppedHandlers.stream().sorted(PostStoppedHandler::compare);
    }
    return Stream.empty();
  }

  /**
   *
   * @return the preStartHandlers
   */
  public Stream<PreStartHandler> getPreStartHandlers() {
    if (!preStartHandlers.isUnsatisfied()) {
      return preStartHandlers.stream().sorted(PreStartHandler::compare);
    }
    return Stream.empty();
  }

  /**
   *
   * @return the preStopHandlers
   */
  public Stream<PreStopHandler> getPreStopHandlers() {
    if (!preStopHandlers.isUnsatisfied()) {
      return preStopHandlers.stream().sorted(PreStopHandler::compare);
    }
    return Stream.empty();
  }

  protected WebServerConfig getConfig() {
    return this.config;
  }

  protected Stream<WebFilterMetaData> getFilterMetaDatas() {
    if (!metaDataProviders.isUnsatisfied()) {
      return metaDataProviders.stream().flatMap(WebMetaDataProvider::filterMetaDataStream);
    } else {
      return Stream.empty();
    }
  }

  protected Stream<WebListenerMetaData> getListenerMetaDatas() {
    if (!metaDataProviders.isUnsatisfied()) {
      return metaDataProviders.stream().flatMap(WebMetaDataProvider::listenerMetaDataStream);
    } else {
      return Stream.empty();
    }
  }

  protected Map<String, Object> getServletContextAttributes() {
    Map<String, Object> map = new HashMap<>();
    if (!metaDataProviders.isUnsatisfied()) {
      metaDataProviders.stream().map(WebMetaDataProvider::servletContextAttributes)
          .forEach(map::putAll);
    }
    return map;
  }

  protected Stream<WebServletMetaData> getServletMetaDatas() {
    if (!metaDataProviders.isUnsatisfied()) {
      return metaDataProviders.stream().flatMap(WebMetaDataProvider::servletMetaDataStream);
    } else {
      return Stream.empty();
    }
  }

  protected KeyManager[] resolveKeyManagers() {
    try {
      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(resolveKeyStore(), config.getKeystorePassword().get().toCharArray());
      return keyManagerFactory.getKeyManagers();
    } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
      throw new CorantRuntimeException(e, "Unable to initialise KeyManager[]");
    }
  }

  protected KeyStore resolveKeyStore() {
    String path = config.getKeystorePath().get();
    String type = config.getKeystoreType().get();
    String password = config.getKeystorePassword().get();
    try (InputStream is = shouldNotNull(AbstractWebServer.class.getResourceAsStream(path),
        "Unable to load keystore, path %s doesn't exists!", path)) {
      KeyStore loadedKeystore = KeyStore.getInstance(type);
      loadedKeystore.load(is, password.toCharArray());
      return loadedKeystore;
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      throw new CorantRuntimeException(e, "Unable to load keystore, path %s doesn't exists!", path);
    }
  }

  protected SSLContext resolveSSLContext() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(resolveKeyManagers(), resolveTrustManagers(), null);
      return sslContext;
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected TrustManager[] resolveTrustManagers() {
    try {
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(resolveTrustStore());
      return trustManagerFactory.getTrustManagers();
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      throw new CorantRuntimeException(e, "Unable to initialise TrustManager[]");
    }
  }

  protected KeyStore resolveTrustStore() {
    String path = config.getTruststorePath().get();
    String type = config.getTruststoreType().orElse("JKS");
    String password = config.getTruststorePassword().get();
    try (InputStream is = shouldNotNull(AbstractWebServer.class.getResourceAsStream(path),
        "Unable to load truststore, path %s doesn't exists!", path)) {
      KeyStore loadedKeystore = KeyStore.getInstance(type);
      loadedKeystore.load(is, password.toCharArray());
      return loadedKeystore;
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      throw new CorantRuntimeException(e, "Unable to load truststore, path %s doesn't exists!",
          path);
    }
  }

  public static class DummyTrustManager implements X509TrustManager {

    public static final DummyTrustManager INSTANCE = new DummyTrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      // FakeInstantOffsetDateTime noop
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      // FakeInstantOffsetDateTime noop
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

  }
}

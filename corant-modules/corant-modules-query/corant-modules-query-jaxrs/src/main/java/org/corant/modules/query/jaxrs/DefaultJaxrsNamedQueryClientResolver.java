/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.defaultBlank;
import static org.corant.shared.util.Strings.isDecimalNumber;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Strings.trim;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.corant.config.Configs;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 15:09:46
 */
@ApplicationScoped
public class DefaultJaxrsNamedQueryClientResolver implements JaxrsNamedQueryClientResolver {

  public static final Map<String, JaxrsNamedQueryClientConfig> configs =
      Configs.resolveMulti(JaxrsNamedQueryClientConfig.class);

  protected Map<String, Client> clients = new ConcurrentHashMap<>();

  @Override
  public Client apply(Query t) {
    return clients.computeIfAbsent(t.getQualifier(), k -> createClient(t));
  }

  @Override
  public void closeAndClear() {
    clients.values().forEach(Client::close);
    clients.clear();
  }

  @Override
  public JaxrsNamedQueryClientConfig getClientConfig(Query query) {
    return shouldNotNull(configs.get(query.getQualifier()), "Client config %s can't find!",
        query.getQualifier());
  }

  protected void appendKeyStoreIFNecessary(JaxrsNamedQueryClientConfig config,
      ClientBuilder builder) {
    // append key store
    if (isNoneBlank(config.getKeyStorePath(), config.getKeyStoreType(),
        config.getKeyStorePassword())) {
      String path = config.getKeyStorePath();
      String type = config.getKeyStoreType();
      String password = config.getKeyStorePassword();
      try (InputStream is =
          shouldNotNull(DefaultJaxrsNamedQueryClientResolver.class.getResourceAsStream(path),
              "Unable to load keystore, path %s doesn't exists!", path)) {
        KeyStore loadedKeystore = KeyStore.getInstance(type);
        builder.keyStore(loadedKeystore, password.toCharArray());
      } catch (IOException | KeyStoreException e) {
        throw new CorantRuntimeException(e, "Unable to load keystore, path %s doesn't exists!",
            path);
      }
    }
  }

  protected void appendRegisterBeanIfNecessary(JaxrsNamedQueryClientConfig config,
      ClientBuilder builder) {
    if (isNotBlank(config.getRegisterBeans())) {
      String[] chunks = split(config.getRegisterBeans(), ";", true, true);
      for (String chunk : chunks) {
        int idx = chunk.indexOf(":");
        if (idx == -1) {
          builder.register(loadBean(trim(chunk)));
        } else if (idx < chunk.length()-1) {
          Object compObj = loadBean(trim(chunk.substring(0, idx)));
          String subs = trim(chunk.substring(idx + 1));
          if (isDecimalNumber(subs)) {
            builder.register(compObj, toInteger(subs));
          } else {
            String[] conts = split(subs, ",", true, true);
            Map<Class<?>, Integer> contMaps = new LinkedHashMap<>();
            for (String cont : conts) {
              String[] cc = split(cont, ":", true, true);
              if (cc.length > 0) {
                Class<?> contractClass = loadClass(cc[0]);
                if (cc.length == 1) {
                  contMaps.put(contractClass, Priorities.USER);
                } else {
                  contMaps.put(contractClass, toInteger(cc[1]));
                }
              }
            }
            if (!contMaps.isEmpty()) {
              builder.register(compObj, contMaps);
            }
          }
        }
      }
    }
  }

  protected void appendRegisterIfNecessary(JaxrsNamedQueryClientConfig config,
      ClientBuilder builder) {
    if (isNotBlank(config.getRegisters())) {
      String[] chunks = split(config.getRegisters(), ";", true, true);
      for (String chunk : chunks) {
        int idx = chunk.indexOf(":");
        if (idx == -1) {
          builder.register(loadClass(trim(chunk)));
        } else if (idx < chunk.length()-1) {
          Class<?> compCls = loadClass(trim(chunk.substring(0, idx)));
          String subs = trim(chunk.substring(idx + 1));
          if (isDecimalNumber(subs)) {
            builder.register(compCls, toInteger(subs));
          } else {
            String[] conts = split(subs, ",", true, true);
            Map<Class<?>, Integer> contMaps = new LinkedHashMap<>();
            for (String cont : conts) {
              String[] cc = split(cont, ":", true, true);
              if (cc.length > 0) {
                Class<?> contractClass = loadClass(cc[0]);
                if (cc.length == 1) {
                  contMaps.put(contractClass, Priorities.USER);
                } else {
                  contMaps.put(contractClass, toInteger(cc[1]));
                }
              }
            }
            if (!contMaps.isEmpty()) {
              builder.register(compCls, contMaps);
            }
          }
        }
      }
    }
  }

  protected void appendTrustStoreIfNecessary(JaxrsNamedQueryClientConfig config,
      ClientBuilder builder) {
    // append trust store
    if (isNoneBlank(config.getTrustStorePath(), config.getTrustStorePassword())) {
      String path = config.getTrustStorePath();
      String type = defaultBlank(config.getTrustStoreType(), "JKS");
      String password = config.getTrustStorePassword();
      try (InputStream is =
          shouldNotNull(DefaultJaxrsNamedQueryClientResolver.class.getResourceAsStream(path),
              "Unable to load truststore, path %s doesn't exists!", path)) {
        KeyStore loadedKeystore = KeyStore.getInstance(type);
        builder.keyStore(loadedKeystore, password.toCharArray());
      } catch (IOException | KeyStoreException e) {
        throw new CorantRuntimeException(e, "Unable to load truststore, path %s doesn't exists!",
            path);
      }
    }
  }

  protected Client createClient(Query query) {
    JaxrsNamedQueryClientConfig config = getClientConfig(query);
    ClientBuilder builder = ClientBuilder.newBuilder();
    if (config.getConnectTimeout() != null) {
      builder.connectTimeout(config.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    if (config.getReadTimeout() != null) {
      builder.readTimeout(config.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    if (isNotEmpty(config.getProperties())) {
      config.getProperties().forEach(builder::property);
    }

    appendKeyStoreIFNecessary(config, builder);
    appendTrustStoreIfNecessary(config, builder);
    appendRegisterIfNecessary(config, builder);
    appendRegisterBeanIfNecessary(config, builder);
    return builder.build();
  }

  protected Object loadBean(String className) {
    return resolve(loadClass(className));
  }

  protected Class<?> loadClass(String className) {
    try {
      return Classes.asClass(className);
    } catch (Exception ex) {
      throw new QueryRuntimeException(ex);
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    closeAndClear();
  }

}

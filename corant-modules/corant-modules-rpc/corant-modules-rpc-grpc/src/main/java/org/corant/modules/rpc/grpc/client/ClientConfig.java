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
package org.corant.modules.rpc.grpc.client;

import static org.corant.shared.util.Strings.isNotBlank;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;

/**
 * corant-modules-rpc-grpc
 *
 * @author bingo 下午5:20:08
 *
 */
@ConfigKeyRoot(value = "corant.rpc.grpc", keyIndex = 3, ignoreNoAnnotatedItem = false)
public class ClientConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = 6342527084682440153L;

  protected String name;
  protected String host;
  @ConfigKeyItem(defaultValue = "9000")
  protected int port;
  protected String certFile;
  protected String keyFile;
  protected String trustManager;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ClientConfig other = (ClientConfig) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the certFile
   */
  public String getCertFile() {
    return certFile;
  }

  /**
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   *
   * @return the keyFile
   */
  public String getKeyFile() {
    return keyFile;
  }

  /**
   *
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   *
   * @return the trustManager
   */
  public String getTrustManager() {
    return trustManager;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    return prime * result + (name == null ? 0 : name.hashCode());
  }

  @Override
  public boolean isValid() {
    return isNotBlank(host);
  }

}

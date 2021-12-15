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
package org.corant.modules.jms.shared;

import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.NamedObject;
import org.corant.modules.jms.JMSConfig;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午10:30:53
 *
 */
@ConfigKeyRoot(value = "corant.jms", keyIndex = 2)
public abstract class AbstractJMSConfig implements JMSConfig, NamedObject, DeclarativeConfig {

  private static final long serialVersionUID = 2263743463205278263L;

  public static final AbstractJMSConfig DFLT_INST = new AbstractJMSConfig() {

    private static final long serialVersionUID = 5340760550873711017L;
  };

  // the connection factory id means a artemis server or cluster
  @ConfigKeyItem
  protected String connectionFactoryId = Qualifiers.EMPTY_NAME;

  @ConfigKeyItem
  protected String username;

  @ConfigKeyItem
  protected String password;

  @ConfigKeyItem
  protected String clientId;

  @ConfigKeyItem
  protected boolean enable = true;

  @ConfigKeyItem
  protected boolean xa = true;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractJMSConfig other = (AbstractJMSConfig) obj;
    if (connectionFactoryId == null) {
      if (other.connectionFactoryId != null) {
        return false;
      }
    } else if (!connectionFactoryId.equals(other.connectionFactoryId)) {
      return false;
    }
    return true;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  /**
   *
   * @return the connectionFactoryId
   */
  @Override
  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  @Override
  public String getName() {
    return connectionFactoryId;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
  }

  @Override
  public boolean isEnable() {
    return enable;
  }

  @Override
  public boolean isXa() {
    return xa;
  }

}

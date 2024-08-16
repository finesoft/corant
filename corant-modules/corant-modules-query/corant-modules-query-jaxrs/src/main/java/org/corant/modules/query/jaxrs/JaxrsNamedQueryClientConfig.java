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

import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import java.util.Map;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 15:14:33
 */
@ConfigKeyRoot(value = "corant.query.jaxrs")
public class JaxrsNamedQueryClientConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = 102901267650664748L;

  @ConfigKeyItem
  protected String root;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> properties;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> headers;

  @ConfigKeyItem
  protected String registers;

  @ConfigKeyItem
  protected String registerBeans;

  @ConfigKeyItem
  protected Duration connectTimeout;

  @ConfigKeyItem
  protected Duration readTimeout;

  @ConfigKeyItem
  protected String keyStorePath;

  @ConfigKeyItem
  protected String keyStoreType;

  @ConfigKeyItem
  protected String keyStorePassword;

  @ConfigKeyItem
  protected String trustStorePath;

  @ConfigKeyItem
  protected String trustStoreType;

  @ConfigKeyItem
  protected String trustStorePassword;

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public String getRegisterBeans() {
    return registerBeans;
  }

  public String getRegisters() {
    return registers;
  }

  public String getRoot() {
    return root;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStoreType() {
    return trustStoreType;
  }

  @Override
  public boolean isValid() {
    return isNotBlank(root);
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }
}

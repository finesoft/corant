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
package org.corant.modules.microprofile.lra;

import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;

/**
 * corant-modules-microprofile-lra
 *
 * @author bingo 下午3:52:27
 */
@ConfigKeyRoot(value = "corant.microprofile.lra", keyIndex = 3)
public class LRAConfig implements DeclarativeConfig {

  private static final long serialVersionUID = -3095817375062098398L;

  public static final LRAConfig EMPTY = new LRAConfig("localhost", 8089);

  @ConfigKeyItem(defaultValue = "localhost")
  protected String host;

  @ConfigKeyItem(defaultValue = "8089")
  protected Integer port;

  public LRAConfig() {}

  public LRAConfig(String host, Integer port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }
}

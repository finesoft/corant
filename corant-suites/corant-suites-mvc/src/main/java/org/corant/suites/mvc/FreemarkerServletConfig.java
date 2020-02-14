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
package org.corant.suites.mvc;

import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.Map;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;

/**
 * corant-suites-mvc
 *
 * @author bingo 下午9:18:05
 *
 */
@ConfigKeyRoot(value = "mvc", keyIndex = 1)
public class FreemarkerServletConfig implements DeclarativeConfig {

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> initParams = new HashMap<>();

  @ConfigKeyItem
  protected String path;

  /**
   *
   * @return the initParams
   */
  public Map<String, String> getInitParams() {
    return initParams;
  }

  /**
   * 
   * @return the path
   */
  public String getPath() {
    return path;
  }

  @Override
  public boolean isValid() {
    return isNotBlank(initParams.get("TemplatePath"));
  }

}

/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.suites.concurrency;

import static org.corant.shared.util.Strings.isNoneBlank;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午3:53:53
 *
 */
@ConfigKeyRoot(value = "concurrent.context.service", ignoreNoAnnotatedItem = false, keyIndex = 3)
public class ContextServiceConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = -5306010134920490346L;

  protected String[] contextInfo;

  @Override
  public boolean isValid() {
    return isNoneBlank(getName());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }
}

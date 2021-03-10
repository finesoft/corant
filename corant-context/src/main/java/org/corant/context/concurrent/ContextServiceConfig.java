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
package org.corant.context.concurrent;

import static org.corant.shared.util.Sets.immutableSetOf;
import static org.corant.shared.util.Strings.isNoneBlank;
import java.util.Set;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.shared.normal.Names;
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

  public static final ContextServiceConfig DFLT_INST = new ContextServiceConfig(Names.CORANT);

  protected Set<ContextInfo> contextInfos = immutableSetOf(ContextInfo.values());

  public ContextServiceConfig() {}

  private ContextServiceConfig(String name) {
    setName(name);
  }

  public Set<ContextInfo> getContextInfos() {
    return contextInfos;
  }

  @Override
  public boolean isValid() {
    return isNoneBlank(getName());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }

  protected void setContextInfos(Set<ContextInfo> contextInfos) {
    this.contextInfos = contextInfos;
  }

  public enum ContextInfo {
    SECURITY, APPLICATION, CDI
  }
}

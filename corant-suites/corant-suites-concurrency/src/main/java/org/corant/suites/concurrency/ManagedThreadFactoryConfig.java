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
 * @author bingo 下午3:48:41
 *
 */
@ConfigKeyRoot(value = "concurrent.thread.factory", ignoreNoAnnotatedItem = false, keyIndex = 3)
public class ManagedThreadFactoryConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = -293822931326474344L;

  private String context;
  private int priority = Thread.NORM_PRIORITY;

  /**
   *
   * @return the context
   */
  public String getContext() {
    return context;
  }

  /**
   *
   * @return the priority
   */
  public int getPriority() {
    return priority;
  }

  @Override
  public boolean isValid() {
    return isNoneBlank(getName(), getContext());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }

  /**
   *
   * @param context the context to set
   */
  protected void setContext(String context) {
    this.context = context;
  }

  /**
   *
   * @param name the name to set
   */
  @Override
  protected void setName(String name) {
    this.name = name;
  }

  /**
   *
   * @param priority the priority to set
   */
  protected void setPriority(int priority) {
    this.priority = priority;
  }

}

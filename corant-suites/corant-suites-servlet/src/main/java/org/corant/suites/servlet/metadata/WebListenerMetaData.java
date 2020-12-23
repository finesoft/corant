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
package org.corant.suites.servlet.metadata;

import java.util.EventListener;

/**
 * corant-suites-servlet
 *
 * @author bingo 上午10:43:46
 *
 */
public class WebListenerMetaData {

  protected Class<? extends EventListener> clazz;

  /**
   * @param clazz
   */
  public WebListenerMetaData(Class<? extends EventListener> clazz) {
    super();
    this.clazz = clazz;
  }

  protected WebListenerMetaData() {}

  /**
   *
   * @return the clazz
   */
  public Class<? extends EventListener> getClazz() {
    return clazz;
  }

}

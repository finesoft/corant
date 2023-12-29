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
package org.corant.modules.jndi;

import javax.naming.InitialContext;
import org.corant.kernel.event.CorantLifecycleEvent;

/**
 * corant-modules-jndi
 *
 * @author bingo 上午11:30:35
 */
public class PostCorantJNDIReadyEvent implements CorantLifecycleEvent {

  private final boolean useCorantContext;

  private final InitialContext context;

  /**
   * @param useCorantContext
   */
  public PostCorantJNDIReadyEvent(boolean useCorantContext, InitialContext context) {
    this.useCorantContext = useCorantContext;
    this.context = context;
  }

  /**
   *
   * @return the context
   */
  public InitialContext getContext() {
    return context;
  }

  /**
   *
   * @return the useCorantContext
   */
  public boolean isUseCorantContext() {
    return useCorantContext;
  }

}

/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.context.security;

import java.security.Principal;
import org.jboss.weld.security.spi.SecurityServices;

/**
 * corant-context
 *
 * @author bingo 下午3:24:05
 *
 */
public class WeldSecurityServices implements SecurityServices {

  @Override
  public void cleanup() {}

  @Override
  public Principal getPrincipal() {
    return (Principal) SecurityContexts.getCurrentPrincipal();
  }

  @Override
  public org.jboss.weld.security.spi.SecurityContext getSecurityContext() {
    return new WeldSecurityContext();
  }

  /**
   * corant-context
   *
   * @author bingo 下午3:26:06
   *
   */
  static class WeldSecurityContext implements org.jboss.weld.security.spi.SecurityContext {

    private final SecurityContext context;
    private SecurityContext oldContext;

    private WeldSecurityContext() {
      this.context = SecurityContexts.getCurrent();
    }

    @Override
    public void associate() {
      if (oldContext != null) {
        throw new IllegalStateException("Security context is already associated");
      }
      oldContext = SecurityContexts.getCurrent();
      SecurityContexts.setCurrent(context);
    }

    @Override
    public void close() {}

    @Override
    public void dissociate() {
      SecurityContexts.setCurrent(oldContext);
      oldContext = null;
    }
  }
}

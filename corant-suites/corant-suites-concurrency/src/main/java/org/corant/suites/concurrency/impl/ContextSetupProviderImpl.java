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
package org.corant.suites.concurrency.impl;

import java.util.Map;
import javax.enterprise.concurrent.ContextService;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午8:44:55
 *
 */
public class ContextSetupProviderImpl implements ContextSetupProvider {

  private static final long serialVersionUID = -5397394660587586147L;

  @Override
  public void reset(ContextHandle contextHandle) {
    // TODO Auto-generated method stub

  }

  @Override
  public ContextHandle saveContext(ContextService contextService) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextHandle saveContext(ContextService contextService,
      Map<String, String> contextObjectProperties) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
    // TODO Auto-generated method stub
    return null;
  }

  public enum ContextType {
    CLASSLOADER, SECURITY, NAMING, WORKAREA
  }
}

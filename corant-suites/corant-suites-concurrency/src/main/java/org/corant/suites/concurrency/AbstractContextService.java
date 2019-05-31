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
package org.corant.suites.concurrency;

import java.util.Map;
import javax.enterprise.concurrent.ContextService;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午8:54:34
 *
 */
public abstract class AbstractContextService implements ContextService {

  @Override
  public Object createContextualProxy(Object instance, Class<?>... interfaces) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object createContextualProxy(Object instance, Map<String, String> executionProperties,
      Class<?>... interfaces) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T createContextualProxy(T instance, Class<T> intf) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T createContextualProxy(T instance, Map<String, String> executionProperties,
      Class<T> intf) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, String> getExecutionProperties(Object contextualProxy) {
    // TODO Auto-generated method stub
    return null;
  }

}

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
package org.corant.suites.jndi;

import javax.naming.Reference;

/**
 * corant-suites-jndi
 *
 * @author bingo 下午3:20:55
 *
 */
public class DefaultReference extends Reference {

  private static final long serialVersionUID = -7231737490239227558L;

  /**
   * @param className
   */
  public DefaultReference(String className) {
    super(className, DefaultObjectFactory.class.getName(), null);
  }

}

/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jndi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * corant-suites-jndi
 *
 * @author bingo 下午2:13:28
 *
 */
public class DefaultInitialContextFactory implements InitialContextFactory {

  protected static volatile Context initialContext = null;

  protected static Context build(Hashtable<?, ?> environment) {
    if (initialContext == null) {
      synchronized (DefaultInitialContextFactory.class) {
        if (initialContext == null) {
          initialContext = new NamingContext(environment);
        }
      }
    }
    return initialContext;
  }

  @Override
  public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
    return build(environment);
  }
}

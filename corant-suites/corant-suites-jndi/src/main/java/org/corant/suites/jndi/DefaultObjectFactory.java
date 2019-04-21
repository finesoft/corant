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

import static org.corant.Corant.instance;
import static org.corant.shared.util.ClassUtils.asClass;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jndi
 *
 * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
 * ApplicationScoped.
 *
 * When InitialContext.lookup(...), will invoke CDI.select() to retrieve the object instance.
 *
 * @author bingo 下午1:03:02
 *
 */
public class DefaultObjectFactory implements ObjectFactory {

  private static final DefaultObjectFactory INST = new DefaultObjectFactory();

  protected static DefaultObjectFactory build(Object obj, Hashtable<?, ?> environment) {
    return INST;
  }

  @Override
  public Object getObjectInstance(Object obj, Name name, Context nameCtx,
      Hashtable<?, ?> environment) throws Exception {
    if (obj instanceof DefaultReference) {
      DefaultReference reference = (DefaultReference) obj;
      Class<?> theClass = asClass(reference.getClassName());
      if (reference.qualifiers.length > 0) {
        return instance().select(theClass).select(reference.qualifiers).get();
      }
      return instance().select(theClass).get();
    } else {
      throw new CorantRuntimeException(
          "Object %s named %s is not a CDI managed bean instance reference!", obj, name);
    }
  }
}

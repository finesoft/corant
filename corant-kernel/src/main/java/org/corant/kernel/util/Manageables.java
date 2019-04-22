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
package org.corant.kernel.util;

import static org.corant.Corant.instance;
import static org.corant.shared.util.ClassUtils.asClass;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.jboss.weld.bean.proxy.ProxyObject;

/**
 * corant-kernel
 *
 * @author bingo 上午11:11:13
 *
 */
public class Manageables {

  // TODO
  public static boolean hasScope(Object object, Annotation scope) {
    return isManagedBean(object);
  }

  // FIXME
  public static boolean isManagedBean(Object object) {
    return ProxyObject.class.isInstance(object);
  }

  /**
   * corant-kernel
   *
   * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
   * ApplicationScoped.
   *
   * When InitialContext.lookup(...), will invoke CDI.select() to retrieve the object instance.
   *
   * @author bingo 下午7:42:18
   *
   */
  public static class NamingObjectFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
        Hashtable<?, ?> environment) throws Exception {
      if (obj instanceof NamingReference) {
        NamingReference reference = (NamingReference) obj;
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

  /**
   * corant-kernel
   *
   * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
   * ApplicationScoped.
   *
   * @author bingo 下午7:42:38
   *
   */
  public static class NamingReference extends Reference {

    private static final long serialVersionUID = -7231737490239227558L;

    protected Annotation[] qualifiers = new Annotation[0];

    /**
     * @param objectClass
     * @param qualifiers
     */
    public NamingReference(Class<?> objectClass, Annotation... qualifiers) {
      super(objectClass.getName(), NamingObjectFactory.class.getName(), null);
      int length;
      if ((length = qualifiers.length) > 0) {
        this.qualifiers = new Annotation[length];
        System.arraycopy(qualifiers, 0, this.qualifiers, 0, length);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      NamingReference other = (NamingReference) obj;
      return Arrays.equals(qualifiers, other.qualifiers);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Arrays.hashCode(qualifiers);
      return result;
    }
  }
}

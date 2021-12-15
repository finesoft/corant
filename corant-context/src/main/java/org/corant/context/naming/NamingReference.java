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
package org.corant.context.naming;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import javax.naming.Reference;
import org.corant.shared.util.Annotations;

/**
 * corant-context
 *
 * Naming reference for CDI managed bean that may have some qualifiers, all bean must be
 * ApplicationScoped.
 *
 * @author bingo 下午7:42:38
 *
 */
public class NamingReference extends Reference {

  private static final long serialVersionUID = -7231737490239227558L;

  protected Annotation[] qualifiers = Annotations.EMPTY_ARRAY;

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
    return prime * result + Arrays.hashCode(qualifiers);
  }
}

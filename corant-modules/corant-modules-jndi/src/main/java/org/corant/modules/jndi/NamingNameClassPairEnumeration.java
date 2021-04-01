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

import java.util.Iterator;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * corant-modules-jndi
 *
 * @author bingo 下午3:13:20
 *
 */
public class NamingNameClassPairEnumeration implements NamingEnumeration<NameClassPair> {

  protected final Iterator<NamingContextEntry> iterator;

  public NamingNameClassPairEnumeration(Iterator<NamingContextEntry> iterator) {
    this.iterator = iterator;
  }

  @Override
  public void close() throws NamingException {}

  @Override
  public boolean hasMore() throws NamingException {
    return iterator.hasNext();
  }

  @Override
  public boolean hasMoreElements() {
    return iterator.hasNext();
  }

  @Override
  public NameClassPair next() throws NamingException {
    return nextElement();
  }

  @Override
  public NameClassPair nextElement() {
    NamingContextEntry entry = iterator.next();
    return new NameClassPair(entry.name, entry.value.getClass().getName());
  }

}

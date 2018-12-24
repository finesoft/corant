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

import java.util.Iterator;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * corant-suites-jndi
 *
 * @author bingo 下午3:13:20
 *
 */
public class NamingContextEnumeration implements NamingEnumeration<NameClassPair> {

  /**
   * Underlying enumeration.
   */
  protected final Iterator<NamingContextEntry> iterator;


  public NamingContextEnumeration(Iterator<NamingContextEntry> iterator) {
    this.iterator = iterator;
  }

  /**
   * Closes this enumeration.
   */
  @Override
  public void close() throws NamingException {}


  /**
   * Determines whether there are any more elements in the enumeration.
   */
  @Override
  public boolean hasMore() throws NamingException {
    return iterator.hasNext();
  }


  @Override
  public boolean hasMoreElements() {
    return iterator.hasNext();
  }


  /**
   * Retrieves the next element in the enumeration.
   */
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

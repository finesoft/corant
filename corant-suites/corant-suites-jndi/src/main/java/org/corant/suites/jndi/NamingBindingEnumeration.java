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

import java.util.Iterator;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * corant-suites-jndi
 *
 * @author bingo 下午3:23:24
 *
 */
public class NamingBindingEnumeration implements NamingEnumeration<Binding> {

  protected final Iterator<NamingContextEntry> iterator;
  private final Context ctx;

  public NamingBindingEnumeration(Iterator<NamingContextEntry> entries, Context ctx) {
    iterator = entries;
    this.ctx = ctx;
  }

  @Override
  public void close() throws NamingException {
    // NOOP
  }

  @Override
  public boolean hasMore() throws NamingException {
    return iterator.hasNext();
  }

  @Override
  public boolean hasMoreElements() {
    return iterator.hasNext();
  }

  @Override
  public Binding next() throws NamingException {
    return nextElementInternal();
  }

  @Override
  public Binding nextElement() {
    try {
      return nextElementInternal();
    } catch (NamingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private Binding nextElementInternal() throws NamingException {
    NamingContextEntry entry = iterator.next();
    Object value;
    // If the entry is a reference, resolve it
    if (entry.type == NamingContextEntry.REFERENCE || entry.type == NamingContextEntry.LINK_REF) {
      try {
        value = ctx.lookup(new CompositeName(entry.name));
      } catch (NamingException e) {
        throw e;
      } catch (Exception e) {
        NamingException ne = new NamingException(e.getMessage());
        ne.initCause(e);
        throw ne;
      }
    } else {
      value = entry.value;
    }

    return new Binding(entry.name, value.getClass().getName(), value, true);
  }
}

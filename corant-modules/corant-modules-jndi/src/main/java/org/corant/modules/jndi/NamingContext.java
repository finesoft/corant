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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;

/**
 * corant-modules-jndi
 *
 * @author bingo 下午7:21:23
 *
 */
public class NamingContext implements Context {

  public static final String DFLT_NME = "corantInitialContext";
  protected static final NameParser nameParser = CompositeName::new;
  protected static final ReentrantReadWriteLock RWL = new ReentrantReadWriteLock();
  protected final Map<String, NamingContextEntry> bindings = new HashMap<>();
  protected final Map<Object, Object> environment = new HashMap<>();
  protected final String name;
  protected volatile boolean closed = false;

  public NamingContext(Hashtable<?, ?> environment) {
    this(DFLT_NME, environment, null);
  }

  public NamingContext(String name) {
    this(name, null, null);
  }

  public NamingContext(String name, Hashtable<?, ?> environment,
      Map<String, NamingContextEntry> bindings) {
    this.name = name;
    if (environment != null) {
      this.environment.putAll(environment);
    }
    if (bindings != null) {
      this.bindings.putAll(bindings);
    }
  }

  @Override
  public Object addToEnvironment(String propName, Object propVal) {
    checkClosed();
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      return environment.put(propName, propVal);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void bind(Name name, Object obj) throws NamingException {
    checkClosed();
    bind(name, obj, false);
  }

  @Override
  public void bind(String name, Object obj) throws NamingException {
    checkClosed();
    bind(nameParser.parse(name), obj);
  }

  @Override
  public void close() throws NamingException {
    // We are in memory so we don't close, bind some object may throw exception.
  }

  @Override
  public Name composeName(Name name, Name prefix) throws NamingException {
    Name usePrefix = (Name) prefix.clone();
    return usePrefix.addAll(name);
  }

  @Override
  public String composeName(String name, String prefix) {
    return prefix + "/" + name;
  }

  @Override
  public Context createSubcontext(Name name) throws NamingException {
    checkClosed();
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      NamingContext subContext = new NamingContext(this.name + ":" + name, getEnvironment(), null);
      bind(name, subContext);
      return subContext;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Context createSubcontext(String name) throws NamingException {
    return createSubcontext(new CompositeName(name));
  }

  @Override
  public void destroySubcontext(Name name) throws NamingException {
    checkClosed();
    Name useName = skipEmptyComponent(name);
    if (useName.isEmpty()) {
      throw new NamingException("Name is not valid");
    }
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (entry == null) {
        throw new NameNotFoundException(
            String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                useName.get(0)));
      }
      if (useName.size() > 1) {
        if (entry.type == NamingContextEntry.CONTEXT) {
          ((Context) entry.value).destroySubcontext(useName.getSuffix(1));
        } else {
          throw new NamingException("Name is not bound to a Context");
        }
      } else if (entry.type == NamingContextEntry.CONTEXT) {
        ((Context) entry.value).close();
        bindings.remove(useName.get(0));
      } else {
        throw new NotContextException("Name is not bound to a Context");
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void destroySubcontext(String name) throws NamingException {
    destroySubcontext(new CompositeName(name));
  }

  @Override
  public Hashtable<?, ?> getEnvironment() {
    checkClosed();
    Lock lock = RWL.readLock();
    lock.lock();
    try {
      return new Hashtable<>(environment);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String getNameInNamespace() throws NamingException {
    throw new OperationNotSupportedException("Cannot generate an absolute name for this namespace");
  }

  @Override
  public NameParser getNameParser(Name name) throws NamingException {
    checkClosed();
    Name useName = skipEmptyComponent(name);
    Lock lock = RWL.readLock();
    lock.lock();
    try {
      if (useName.size() > 1) {
        Object obj = bindings.get(useName.get(0));
        if (obj instanceof Context) {
          return ((Context) obj).getNameParser(useName.getSuffix(1));
        } else {
          throw new NotContextException("Name is not bound to a Context");
        }
      } else {
        return nameParser;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public NameParser getNameParser(String name) throws NamingException {
    return getNameParser(new CompositeName(name));
  }

  @Override
  public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
    checkClosed();
    Name useName = skipEmptyComponent(name);
    if (useName.isEmpty()) {
      return new NamingNameClassPairEnumeration(bindings.values().iterator());
    }
    Lock lock = RWL.readLock();
    lock.lock();
    try {
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (entry == null) {
        throw new NameNotFoundException(
            String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                useName.get(0)));
      }
      if (entry.type != NamingContextEntry.CONTEXT) {
        throw new NamingException("Name is not bound to a Context");
      }
      return ((Context) entry.value).list(useName.getSuffix(1));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
    return list(new CompositeName(name));
  }

  @Override
  public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
    checkClosed();
    Name useName = skipEmptyComponent(name);
    if (useName.isEmpty()) {
      return new NamingBindingEnumeration(bindings.values().iterator(), this);
    }
    Lock lock = RWL.readLock();
    lock.lock();
    try {
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (entry == null) {
        throw new NameNotFoundException(
            String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                useName.get(0)));
      }
      if (entry.type != NamingContextEntry.CONTEXT) {
        throw new NamingException("Name is not bound to a Context");
      }
      return ((Context) entry.value).listBindings(useName.getSuffix(1));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
    return listBindings(new CompositeName(name));
  }

  @Override
  public Object lookup(Name name) throws NamingException {
    return lookup(name, true);
  }

  @Override
  public Object lookup(String name) throws NamingException {
    return lookup(new CompositeName(name), true);
  }

  @Override
  public Object lookupLink(Name name) throws NamingException {
    return lookup(name, false);
  }

  @Override
  public Object lookupLink(String name) throws NamingException {
    return lookup(new CompositeName(name), false);
  }

  @Override
  public void rebind(Name name, Object obj) throws NamingException {
    bind(name, obj, true);
  }

  @Override
  public void rebind(String name, Object obj) throws NamingException {
    rebind(new CompositeName(name), obj);
  }

  public void release() {
    if (!closed) {
      Lock lock = RWL.writeLock();
      lock.lock();
      try {
        closed = true;
        environment.clear();
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public Object removeFromEnvironment(String propName) {
    checkClosed();
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      return environment.remove(propName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void rename(Name oldName, Name newName) throws NamingException {
    checkClosed();
    Object value = lookup(oldName);
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      bind(newName, value);
      unbind(oldName);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void rename(String oldName, String newName) throws NamingException {
    checkClosed();
    rename(new CompositeName(oldName), new CompositeName(newName));
  }

  @Override
  public void unbind(Name name) throws NamingException {
    checkClosed();
    Name useName = skipEmptyComponent(name);
    if (useName.isEmpty()) {
      throw new NamingException("Name is not valid");
    }
    Lock lock = RWL.writeLock();
    lock.lock();
    try {
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (entry == null) {
        throw new NameNotFoundException(
            String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                useName.get(0)));
      }
      if (useName.size() > 1) {
        if (entry.type == NamingContextEntry.CONTEXT) {
          ((Context) entry.value).unbind(useName.getSuffix(1));
        } else {
          throw new NamingException("Name is not bound to a Context");
        }
      } else {
        bindings.remove(useName.get(0));
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void unbind(String name) throws NamingException {
    unbind(new CompositeName(name));
  }

  /**
   * Binds a name to an object. All intermediate contexts and the target context (that named by all
   * but terminal atomic component of the name) must already exist.
   *
   * @param name the name to bind; may not be empty
   * @param obj the object to bind; possibly null
   * @param rebind if true, then perform a rebind (ie, overwrite)
   * @exception NameAlreadyBoundException if name is already bound
   * @exception javax.naming.directory.InvalidAttributesException if object did not supply all
   *            mandatory attributes
   * @exception NamingException if a naming exception is encountered
   */
  protected void bind(Name name, Object obj, boolean rebind) throws NamingException {
    checkClosed();
    checkBindingObject(obj);
    Name useName = skipEmptyComponent(name);
    if (useName.isEmpty()) {
      throw new NamingException("Name is not valid, can not be empty.");
    }
    Lock lock = RWL.writeLock();
    try {
      lock.lock();
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (useName.size() > 1) {
        if (entry == null) {
          throw new NameNotFoundException(
              String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                  useName.get(0)));
        } else if (entry.type != NamingContextEntry.CONTEXT) {
          throw new NamingException("Name must be bound to a Context, since it has children.");
        }
        if (rebind) {
          ((Context) entry.value).rebind(useName.getSuffix(1), obj);
        } else {
          ((Context) entry.value).bind(useName.getSuffix(1), obj);
        }
      } else if (!rebind && entry != null) {
        throw new NameAlreadyBoundException(
            String.format("Name [%s] is already bound in this Context", useName.get(0)));
      } else {
        Object toBind = NamingManager.getStateToBind(obj, useName, this, getEnvironment());
        if (toBind instanceof Context) {
          entry = new NamingContextEntry(useName.get(0), toBind, NamingContextEntry.CONTEXT);
        } else if (toBind instanceof LinkRef) {
          entry = new NamingContextEntry(useName.get(0), toBind, NamingContextEntry.LINK_REF);
        } else if (toBind instanceof Reference) {
          entry = new NamingContextEntry(useName.get(0), toBind, NamingContextEntry.REFERENCE);
        } else if (toBind instanceof Referenceable) {
          toBind = ((Referenceable) toBind).getReference();
          entry = new NamingContextEntry(useName.get(0), toBind, NamingContextEntry.REFERENCE);
        } else {
          entry = new NamingContextEntry(useName.get(0), toBind, NamingContextEntry.ENTRY);
        }
        bindings.put(useName.get(0), entry);
      }
    } finally {
      lock.unlock();
    }
  }

  protected void checkBindingObject(Object obj) {
    // TODO check if object is cdi managed bean than check its scope,
    // we only receive ApplicationScope or the bean create by service registry.
  }

  /**
   * Retrieves the named object.
   *
   * @param name the name of the object to look up
   * @param resolveLinks If true, the links will be resolved
   * @return the object bound to name
   * @exception NamingException if a naming exception is encountered
   */
  protected Object lookup(Name name, boolean resolveLinks) throws NamingException {
    Name useName = skipEmptyComponent(name);
    Lock lock = RWL.readLock();
    try {
      lock.lock();
      if (useName.isEmpty()) {
        return new NamingContext(this.name, getEnvironment(), bindings);
      }
      NamingContextEntry entry = bindings.get(useName.get(0));
      if (entry == null) {
        throw new NameNotFoundException(
            String.format("Name [%s] is not bound in this Context. Unable to find [%s]", useName,
                useName.get(0)));
      }
      if (useName.size() > 1) {
        // lookup subcontexts.
        if (entry.type != NamingContextEntry.CONTEXT) {
          throw new NamingException("Name must be bound to a Context, since it has children.");
        }
        return ((Context) entry.value).lookup(useName.getSuffix(1));
      } else if (resolveLinks && entry.type == NamingContextEntry.LINK_REF) {
        String link = ((LinkRef) entry.value).getLinkName();
        if (!link.isEmpty() && link.charAt(0) == '.') {
          return lookup(link.substring(1));// Link relative to this context
        } else {
          return new InitialContext(getEnvironment()).lookup(link);
        }
      } else if (entry.type == NamingContextEntry.REFERENCE) {
        try {
          return NamingManager.getObjectInstance(entry.value, useName, this, getEnvironment());
        } catch (NamingException e) {
          throw e;
        } catch (Exception e) {
          NamingException ne = new NamingException("Unexpected exception resolving reference");
          ne.initCause(e);
          throw ne;
        }
      } else {
        return entry.value;
      }
    } finally {
      lock.unlock();
    }
  }

  private void checkClosed() {
    shouldBeFalse(closed,
        "The context has been closed, invoking any other methodon a closed context is not allowed");
  }

  // skip "///x" {"","","","x"}
  private Name skipEmptyComponent(Name name) {
    Name useName = shouldNotNull(name);
    while (!useName.isEmpty() && useName.get(0).length() == 0) {
      useName = useName.getSuffix(1);
    }
    return useName;
  }

}

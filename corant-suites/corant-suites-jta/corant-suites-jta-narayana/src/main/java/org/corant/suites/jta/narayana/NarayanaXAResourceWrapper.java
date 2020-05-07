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
package org.corant.suites.jta.narayana;

import static org.corant.shared.util.Assertions.shouldNotNull;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.jboss.tm.XAResourceWrapper;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午10:17:57
 *
 */
public class NarayanaXAResourceWrapper implements XAResourceWrapper {

  final XAResource resource;
  final String jndiName;
  final String productName;
  final String productVersion;

  /**
   * @param resource
   * @param jndiName
   * @param productName
   * @param productVersion
   */
  public NarayanaXAResourceWrapper(XAResource resource, String jndiName, String productName,
      String productVersion) {
    super();
    this.resource = shouldNotNull(resource);
    this.jndiName = jndiName;
    this.productName = productName;
    this.productVersion = productVersion;
  }

  @Override
  public void commit(Xid xid, boolean onePhase) throws XAException {
    resource.commit(xid, onePhase);
  }

  @Override
  public void end(Xid xid, int flags) throws XAException {
    resource.end(xid, flags);
  }

  @Override
  public void forget(Xid xid) throws XAException {
    resource.forget(xid);
  }

  @Override
  public String getJndiName() {
    return jndiName;
  }

  @Override
  public String getProductName() {
    return productName;
  }

  @Override
  public String getProductVersion() {
    return productVersion;
  }

  @Override
  public XAResource getResource() {
    return resource;
  }

  @Override
  public int getTransactionTimeout() throws XAException {
    return resource.getTransactionTimeout();
  }

  @Override
  public boolean isSameRM(XAResource xares) throws XAException {
    return resource.isSameRM(xares);
  }

  @Override
  public int prepare(Xid xid) throws XAException {
    return resource.prepare(xid);
  }

  @Override
  public Xid[] recover(int flag) throws XAException {
    return resource.recover(flag);
  }

  @Override
  public void rollback(Xid xid) throws XAException {
    resource.rollback(xid);
  }

  @Override
  public boolean setTransactionTimeout(int seconds) throws XAException {
    return resource.setTransactionTimeout(seconds);
  }

  @Override
  public void start(Xid xid, int flags) throws XAException {
    resource.start(xid, flags);
  }

}

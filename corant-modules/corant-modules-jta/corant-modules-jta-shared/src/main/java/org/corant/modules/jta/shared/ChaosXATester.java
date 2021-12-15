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
package org.corant.modules.jta.shared;

import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jta-shared
 *
 * Use for testing
 *
 * @author bingo 上午11:04:13
 *
 */
public class ChaosXATester {

  final TransactionService transactionService;
  final List<Runnable> runners;

  /**
   * @param transactionService
   * @param runners
   */
  public ChaosXATester(TransactionService transactionService, List<Runnable> runners) {
    this.transactionService = transactionService;
    this.runners = runners;
  }

  public void test(boolean halt) {
    if (halt) {
      failure();
    } else {
      normal();
    }
  }

  protected void failure() {
    try {
      // transactionService.enlistXAResource(new ChaosXAResource(ChaosXAResource.faultType.NONE));
      transactionService.enlistXAResource(new ChaosXAResource(ChaosXAResource.faultType.HALT));
      runners.forEach(Runnable::run);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected void normal() {
    runners.forEach(Runnable::run);
  }

  public static class ChaosXAResource implements XAResource, Serializable {

    private static final long serialVersionUID = -115520889587270637L;

    private static int commitRequests = 0;

    private transient faultType fault = faultType.NONE;

    private Xid[] recoveryXids;

    public boolean startCalled;

    public boolean endCalled;

    public boolean prepareCalled;

    public boolean commitCalled;

    public boolean rollbackCalled;

    public boolean forgetCalled;

    public boolean recoverCalled;

    public ChaosXAResource() {
      this(faultType.NONE);
    }

    public ChaosXAResource(faultType fault) {
      this.fault = fault;
    }

    public static int getCommitRequests() {
      return commitRequests;
    }

    @Override
    public void commit(final Xid xid, final boolean arg1) throws XAException {
      System.out.println("ChaosXAResource commit() called, fault: " + fault + " xid: " + xid);
      commitCalled = true;
      commitRequests += 1;

      if (fault != null) {
        if (faultType.EX.equals(fault)) {
          throw new XAException(XAException.XA_RBTRANSIENT);
        } else if (faultType.HALT.equals(fault)) {
          recoveryXids = new Xid[1];
          recoveryXids[0] = xid;
          Runtime.getRuntime().halt(1);
        }
      }
    }

    @Override
    public void end(final Xid xid, final int arg1) throws XAException {
      endCalled = true;
    }

    @Override
    public void forget(final Xid xid) throws XAException {
      forgetCalled = true;
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      return 0;
    }

    @Override
    public boolean isSameRM(final XAResource arg0) throws XAException {
      return equals(arg0);
    }

    @Override
    public int prepare(final Xid xid) throws XAException {
      prepareCalled = true;
      return XAResource.XA_OK;
    }

    @Override
    public Xid[] recover(final int arg0) throws XAException {
      recoverCalled = true;
      return recoveryXids;
    }

    @Override
    public void rollback(final Xid xid) throws XAException {
      rollbackCalled = true;
    }

    @Override
    public boolean setTransactionTimeout(final int arg0) throws XAException {
      return false;
    }

    @Override
    public void start(final Xid xid, final int arg1) throws XAException {
      startCalled = true;
    }

    public enum faultType {
      HALT, EX, NONE
    }
  }

  public static class ChaosXid implements Xid {

    private final byte[] branchQualifier;

    private final int formatId;

    private final byte[] globalTransactionId;

    private int hash;

    private boolean hashCalculated;

    /**
     * Standard constructor
     *
     * @param branchQualifier
     * @param formatId
     * @param globalTransactionId
     */
    public ChaosXid(final byte[] branchQualifier, final int formatId,
        final byte[] globalTransactionId) {
      this.branchQualifier = branchQualifier;
      this.formatId = formatId;
      this.globalTransactionId = globalTransactionId;
    }

    /**
     * Copy constructor
     *
     * @param other
     */
    public ChaosXid(final Xid other) {
      branchQualifier = copyBytes(other.getBranchQualifier());
      formatId = other.getFormatId();
      globalTransactionId = copyBytes(other.getGlobalTransactionId());
    }

    public static String toBase64String(final Xid xid) {
      return Base64.getEncoder().encodeToString(ChaosXid.toByteArray(xid));
    }

    private static byte[] toByteArray(final Xid xid) {
      byte[] branchQualifier = xid.getBranchQualifier();
      byte[] globalTransactionId = xid.getGlobalTransactionId();
      int formatId = xid.getFormatId();

      byte[] hashBytes = new byte[branchQualifier.length + globalTransactionId.length + 4];
      System.arraycopy(branchQualifier, 0, hashBytes, 0, branchQualifier.length);
      System.arraycopy(globalTransactionId, 0, hashBytes, branchQualifier.length,
          globalTransactionId.length);
      byte[] intBytes = new byte[4];
      for (int i = 0; i < 4; i++) {
        intBytes[i] = (byte) ((formatId >> i * 8) % 0xFF);
      }
      System.arraycopy(intBytes, 0, hashBytes, branchQualifier.length + globalTransactionId.length,
          4);
      return hashBytes;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof Xid)) {
        return false;
      }
      Xid xother = (Xid) other;
      if (xother.getFormatId() != formatId) {
        return false;
      }
      if (xother.getBranchQualifier().length != branchQualifier.length) {
        return false;
      }
      if (xother.getGlobalTransactionId().length != globalTransactionId.length) {
        return false;
      }
      for (int i = 0; i < branchQualifier.length; i++) {
        byte[] otherBQ = xother.getBranchQualifier();
        if (branchQualifier[i] != otherBQ[i]) {
          return false;
        }
      }
      for (int i = 0; i < globalTransactionId.length; i++) {
        byte[] otherGtx = xother.getGlobalTransactionId();
        if (globalTransactionId[i] != otherGtx[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public byte[] getBranchQualifier() {
      return branchQualifier;
    }

    @Override
    public int getFormatId() {
      return formatId;
    }

    @Override
    public byte[] getGlobalTransactionId() {
      return globalTransactionId;
    }

    @Override
    public int hashCode() {
      if (!hashCalculated) {
        calcHash();
      }
      return hash;
    }

    @Override
    public String toString() {
      return "ChaosXid (" + System.identityHashCode(this) + " bq:" + stringRep(branchQualifier)
          + " formatID:" + formatId + " gtxid:" + stringRep(globalTransactionId);
    }

    private void calcHash() {
      byte[] hashBytes = ChaosXid.toByteArray(this);
      String s = new String(hashBytes);
      hash = s.hashCode();
      hashCalculated = true;
    }

    private byte[] copyBytes(final byte[] other) {
      byte[] bytes = new byte[other.length];

      System.arraycopy(other, 0, bytes, 0, other.length);

      return bytes;
    }

    private String stringRep(final byte[] bytes) {
      StringBuilder buff = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        byte b = bytes[i];

        buff.append(b);

        if (i != bytes.length - 1) {
          buff.append('.');
        }
      }

      return buff.toString();
    }
  }
}

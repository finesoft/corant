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

import java.util.Optional;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午7:56:21
 *
 */
public class NarayanaConfig {

  private Integer transactionTimeout;

  private Optional<Integer> transactionStatusManagerPort;

  private Optional<String> transactionStatusManagerAddress;

  private Boolean transactionSync = true;

  private Integer socketProcessIdPort;

  private Optional<String> nodeIdentifier;

  private Optional<String> xaResourceOrphanFilterClassNames;

  private Optional<String> expiryScannerClassNames;

  private Boolean commitOnePhase = true;

  private Optional<String> xaRecoveryNodes;

  private Integer periodicRecoveryPeriod;

  private Integer recoveryBackoffPeriod;

  private Optional<Integer> recoveryPort;

  private Optional<String> recoveryAddress;

  private Optional<String> recoveryModuleClassNames;

  private Optional<Boolean> recoveryListener;

  private Optional<String> objectStoreDir;

  /**
   * @param transactionTimeout
   * @param transactionStatusManagerPort
   * @param transactionStatusManagerAddress
   * @param transactionSync
   * @param socketProcessIdPort
   * @param nodeIdentifier
   * @param xaResourceOrphanFilterClassNames
   * @param expiryScannerClassNames
   * @param commitOnePhase
   * @param xaRecoveryNodes
   * @param periodicRecoveryPeriod
   * @param recoveryBackoffPeriod
   * @param recoveryPort
   * @param recoveryAddress
   * @param recoveryModuleClassNames
   * @param recoveryListener
   * @param objectStoreDir
   */
  public NarayanaConfig(Integer transactionTimeout, Optional<Integer> transactionStatusManagerPort,
      Optional<String> transactionStatusManagerAddress, Boolean transactionSync,
      Integer socketProcessIdPort, Optional<String> nodeIdentifier,
      Optional<String> xaResourceOrphanFilterClassNames, Optional<String> expiryScannerClassNames,
      Boolean commitOnePhase, Optional<String> xaRecoveryNodes, Integer periodicRecoveryPeriod,
      Integer recoveryBackoffPeriod, Optional<Integer> recoveryPort,
      Optional<String> recoveryAddress, Optional<String> recoveryModuleClassNames,
      Optional<Boolean> recoveryListener, Optional<String> objectStoreDir) {
    super();
    this.transactionTimeout = transactionTimeout;
    this.transactionStatusManagerPort = transactionStatusManagerPort;
    this.transactionStatusManagerAddress = transactionStatusManagerAddress;
    this.transactionSync = transactionSync;
    this.socketProcessIdPort = socketProcessIdPort;
    this.nodeIdentifier = nodeIdentifier;
    this.xaResourceOrphanFilterClassNames = xaResourceOrphanFilterClassNames;
    this.expiryScannerClassNames = expiryScannerClassNames;
    this.commitOnePhase = commitOnePhase;
    this.xaRecoveryNodes = xaRecoveryNodes;
    this.periodicRecoveryPeriod = periodicRecoveryPeriod;
    this.recoveryBackoffPeriod = recoveryBackoffPeriod;
    this.recoveryPort = recoveryPort;
    this.recoveryAddress = recoveryAddress;
    this.recoveryModuleClassNames = recoveryModuleClassNames;
    this.recoveryListener = recoveryListener;
    this.objectStoreDir = objectStoreDir;
  }

  protected NarayanaConfig() {

  }

  public static NarayanaConfig of(Config config) {
    NarayanaConfig nf = new NarayanaConfig();
    nf.setTransactionTimeout(
        config.getOptionalValue("jta.transaction.timeout", Integer.class).orElse(0));
    nf.setTransactionStatusManagerPort(config
        .getOptionalValue("jta.transaction.narayana.transactionStatusManagerPort", Integer.class));
    nf.setTransactionStatusManagerAddress(config.getOptionalValue(
        "jta.transaction.narayana.transactionStatusManagerAddress", String.class));
    nf.setTransactionSync(config
        .getOptionalValue("jta.transaction.narayana.transactionSync", Boolean.class).orElse(true));
    nf.setSocketProcessIdPort(config
        .getOptionalValue("jta.transaction.narayana.socketProcessIdPort", Integer.class).orElse(0));
    nf.setNodeIdentifier(
        config.getOptionalValue("jta.transaction.narayana.nodeIdentifier", String.class));
    nf.setXaResourceOrphanFilterClassNames(config.getOptionalValue(
        "jta.transaction.narayana.xaResourceOrphanFilterClassNames", String.class));
    nf.setExpiryScannerClassNames(
        config.getOptionalValue("jta.transaction.narayana.expiryScannerClassNames", String.class));
    nf.setCommitOnePhase(config
        .getOptionalValue("jta.transaction.narayana.commitOnePhase", Boolean.class).orElse(true));
    nf.setXaRecoveryNodes(
        config.getOptionalValue("jta.transaction.narayana.xaRecoveryNodes", String.class));
    nf.setPeriodicRecoveryPeriod(
        config.getOptionalValue("jta.transaction.narayana.periodicRecoveryPeriod", Integer.class)
            .orElse(120));
    nf.setRecoveryBackoffPeriod(
        config.getOptionalValue("jta.transaction.narayana.recoveryBackoffPeriod", Integer.class)
            .orElse(10));
    nf.setRecoveryPort(
        config.getOptionalValue("jta.transaction.narayana.recoveryPort", Integer.class));
    nf.setRecoveryAddress(
        config.getOptionalValue("jta.transaction.narayana.recoveryAddress", String.class));
    nf.setRecoveryModuleClassNames(
        config.getOptionalValue("jta.transaction.narayana.recoveryModuleClassNames", String.class));
    nf.setRecoveryListener(
        config.getOptionalValue("jta.transaction.narayana.recoveryListener", Boolean.class));
    nf.setObjectStoreDir(
        config.getOptionalValue("jta.transaction.narayana.object-store-dir", String.class));
    return nf;
  }

  /**
   *
   * @return the commitOnePhase
   */
  public Boolean getCommitOnePhase() {
    return commitOnePhase;
  }

  /**
   *
   * @return the expiryScannerClassNames
   */
  public Optional<String> getExpiryScannerClassNames() {
    return expiryScannerClassNames;
  }

  /**
   *
   * @return the nodeIdentifier
   */
  public Optional<String> getNodeIdentifier() {
    return nodeIdentifier;
  }

  /**
   *
   * @return the objectStoreDir
   */
  public Optional<String> getObjectStoreDir() {
    return objectStoreDir;
  }

  /**
   *
   * @return the periodicRecoveryPeriod
   */
  public Integer getPeriodicRecoveryPeriod() {
    return periodicRecoveryPeriod;
  }

  /**
   *
   * @return the recoveryAddress
   */
  public Optional<String> getRecoveryAddress() {
    return recoveryAddress;
  }

  /**
   *
   * @return the recoveryBackoffPeriod
   */
  public Integer getRecoveryBackoffPeriod() {
    return recoveryBackoffPeriod;
  }

  /**
   *
   * @return the recoveryListener
   */
  public Optional<Boolean> getRecoveryListener() {
    return recoveryListener;
  }

  /**
   *
   * @return the recoveryModuleClassNames
   */
  public Optional<String> getRecoveryModuleClassNames() {
    return recoveryModuleClassNames;
  }

  /**
   *
   * @return the recoveryPort
   */
  public Optional<Integer> getRecoveryPort() {
    return recoveryPort;
  }

  /**
   *
   * @return the socketProcessIdPort
   */
  public Integer getSocketProcessIdPort() {
    return socketProcessIdPort;
  }

  /**
   *
   * @return the transactionStatusManagerAddress
   */
  public Optional<String> getTransactionStatusManagerAddress() {
    return transactionStatusManagerAddress;
  }

  /**
   *
   * @return the transactionStatusManagerPort
   */
  public Optional<Integer> getTransactionStatusManagerPort() {
    return transactionStatusManagerPort;
  }

  /**
   *
   * @return the transactionSync
   */
  public Boolean getTransactionSync() {
    return transactionSync;
  }

  /**
   *
   * @return the transactionTimeout
   */
  public Integer getTransactionTimeout() {
    return transactionTimeout;
  }

  /**
   *
   * @return the xaRecoveryNodes
   */
  public Optional<String> getXaRecoveryNodes() {
    return xaRecoveryNodes;
  }

  /**
   *
   * @return the xaResourceOrphanFilterClassNames
   */
  public Optional<String> getXaResourceOrphanFilterClassNames() {
    return xaResourceOrphanFilterClassNames;
  }

  /**
   *
   * @param commitOnePhase the commitOnePhase to set
   */
  protected void setCommitOnePhase(Boolean commitOnePhase) {
    this.commitOnePhase = commitOnePhase;
  }

  /**
   *
   * @param expiryScannerClassNames the expiryScannerClassNames to set
   */
  protected void setExpiryScannerClassNames(Optional<String> expiryScannerClassNames) {
    this.expiryScannerClassNames = expiryScannerClassNames;
  }

  /**
   *
   * @param nodeIdentifier the nodeIdentifier to set
   */
  protected void setNodeIdentifier(Optional<String> nodeIdentifier) {
    this.nodeIdentifier = nodeIdentifier;
  }

  /**
   *
   * @param objectStoreDir the objectStoreDir to set
   */
  protected void setObjectStoreDir(Optional<String> objectStoreDir) {
    this.objectStoreDir = objectStoreDir;
  }

  /**
   *
   * @param periodicRecoveryPeriod the periodicRecoveryPeriod to set
   */
  protected void setPeriodicRecoveryPeriod(Integer periodicRecoveryPeriod) {
    this.periodicRecoveryPeriod = periodicRecoveryPeriod;
  }

  /**
   *
   * @param recoveryAddress the recoveryAddress to set
   */
  protected void setRecoveryAddress(Optional<String> recoveryAddress) {
    this.recoveryAddress = recoveryAddress;
  }

  /**
   *
   * @param recoveryBackoffPeriod the recoveryBackoffPeriod to set
   */
  protected void setRecoveryBackoffPeriod(Integer recoveryBackoffPeriod) {
    this.recoveryBackoffPeriod = recoveryBackoffPeriod;
  }

  /**
   *
   * @param recoveryListener the recoveryListener to set
   */
  protected void setRecoveryListener(Optional<Boolean> recoveryListener) {
    this.recoveryListener = recoveryListener;
  }

  /**
   *
   * @param recoveryModuleClassNames the recoveryModuleClassNames to set
   */
  protected void setRecoveryModuleClassNames(Optional<String> recoveryModuleClassNames) {
    this.recoveryModuleClassNames = recoveryModuleClassNames;
  }

  /**
   *
   * @param recoveryPort the recoveryPort to set
   */
  protected void setRecoveryPort(Optional<Integer> recoveryPort) {
    this.recoveryPort = recoveryPort;
  }

  /**
   *
   * @param socketProcessIdPort the socketProcessIdPort to set
   */
  protected void setSocketProcessIdPort(Integer socketProcessIdPort) {
    this.socketProcessIdPort = socketProcessIdPort;
  }

  /**
   *
   * @param transactionStatusManagerAddress the transactionStatusManagerAddress to set
   */
  protected void setTransactionStatusManagerAddress(
      Optional<String> transactionStatusManagerAddress) {
    this.transactionStatusManagerAddress = transactionStatusManagerAddress;
  }

  /**
   *
   * @param transactionStatusManagerPort the transactionStatusManagerPort to set
   */
  protected void setTransactionStatusManagerPort(Optional<Integer> transactionStatusManagerPort) {
    this.transactionStatusManagerPort = transactionStatusManagerPort;
  }

  /**
   *
   * @param transactionSync the transactionSync to set
   */
  protected void setTransactionSync(Boolean transactionSync) {
    this.transactionSync = transactionSync;
  }

  /**
   *
   * @param transactionTimeout the transactionTimeout to set
   */
  protected void setTransactionTimeout(Integer transactionTimeout) {
    this.transactionTimeout = transactionTimeout;
  }

  /**
   *
   * @param xaRecoveryNodes the xaRecoveryNodes to set
   */
  protected void setXaRecoveryNodes(Optional<String> xaRecoveryNodes) {
    this.xaRecoveryNodes = xaRecoveryNodes;
  }

  /**
   *
   * @param xaResourceOrphanFilterClassNames the xaResourceOrphanFilterClassNames to set
   */
  protected void setXaResourceOrphanFilterClassNames(
      Optional<String> xaResourceOrphanFilterClassNames) {
    this.xaResourceOrphanFilterClassNames = xaResourceOrphanFilterClassNames;
  }

}

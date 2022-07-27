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
package org.corant.context.concurrent.provider;

import javax.transaction.Transaction;
import org.corant.context.Contexts.ContextInstaller;
import org.corant.context.Contexts.ContextRestorer;
import org.corant.context.security.SecurityContext;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;

/**
 * corant-context
 *
 * @author bingo 下午9:25:41
 *
 */
public class ContextHandleImpl implements ContextHandle {

  private static final long serialVersionUID = 4619888829612192057L;

  private transient ContextInstaller CDIContextInstaller;
  private transient ContextRestorer CDIContextRestorer;
  private transient ClassLoader contextClassLoader;
  private transient Transaction transaction;
  private SecurityContext securityContext;
  private boolean useTransactionOfExecutionThread;

  /**
   * @return the cDIContextInstaller
   */
  public ContextInstaller getCDIContextInstaller() {
    return CDIContextInstaller;
  }

  /**
   * @return the cDIContextRestorer
   */
  public ContextRestorer getCDIContextRestorer() {
    return CDIContextRestorer;
  }

  /**
   * @return the contextClassLoader
   */
  public ClassLoader getContextClassLoader() {
    return contextClassLoader;
  }

  /**
   * @return the securityContext
   */
  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  /**
   * @return the useTransactionOfExecutionThread
   */
  public boolean isUseTransactionOfExecutionThread() {
    return useTransactionOfExecutionThread;
  }

  /**
   * @param cDIContextInstaller the cDIContextInstaller to set
   */
  protected void setCDIContextInstaller(ContextInstaller cDIContextInstaller) {
    CDIContextInstaller = cDIContextInstaller;
  }

  /**
   * @param cDIContextRestorer the cDIContextRestorer to set
   */
  protected void setCDIContextRestorer(ContextRestorer cDIContextRestorer) {
    CDIContextRestorer = cDIContextRestorer;
  }

  /**
   *
   * @param contextClassLoader the contextClassLoader to set
   */
  protected void setContextClassLoader(ClassLoader contextClassLoader) {
    this.contextClassLoader = contextClassLoader;
  }

  /**
   * @param securityContext the securityContext to set
   */
  protected void setSecurityContext(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  protected void setTransaction(Transaction transaction) {
    this.transaction = transaction;
  }

  /**
   * @param useTransactionOfExecutionThread the useTransactionOfExecutionThread to set
   */
  protected void setUseTransactionOfExecutionThread(boolean useTransactionOfExecutionThread) {
    this.useTransactionOfExecutionThread = useTransactionOfExecutionThread;
  }

}

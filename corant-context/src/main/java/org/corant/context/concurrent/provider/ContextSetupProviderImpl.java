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

import static java.lang.String.format;
import static org.corant.context.Beans.tryResolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Sets.immutableSetOf;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.corant.context.Contexts.ContextInstaller;
import org.corant.context.concurrent.ContextServiceConfig.ContextInfo;
import org.corant.context.security.SecurityContexts;
import org.corant.shared.exception.CorantRuntimeException;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-context
 *
 * @author bingo 下午8:44:55
 */
public class ContextSetupProviderImpl implements ContextSetupProvider {

  private static final long serialVersionUID = -5397394660587586147L;

  static final Logger logger = Logger.getLogger(ContextSetupProviderImpl.class.getName());

  final Set<ContextInfo> contextInfos;

  public ContextSetupProviderImpl(ContextInfo... infos) {
    contextInfos = immutableSetOf(infos);
  }

  @Override
  public void reset(ContextHandle contextHandle) {
    shouldBeTrue(contextHandle instanceof ContextHandleImpl);
    ContextHandleImpl resetContextHandle = (ContextHandleImpl) contextHandle;
    resetCDIContext(resetContextHandle);
    resetSecurityContext(resetContextHandle);
    resetApplicationContext(resetContextHandle);
    // FIXME the concurrency-ri doesn't handle additional information when executing task.
    // resetTranction(resetContextHandle);
  }

  @Override
  public ContextHandle saveContext(ContextService contextService) {
    return saveContext(contextService, null);
  }

  @Override
  public ContextHandle saveContext(ContextService contextService,
      Map<String, String> contextObjectProperties) {
    ContextHandleImpl contextHandle = new ContextHandleImpl();
    saveApplicationContext(contextHandle, contextService, contextObjectProperties);
    saveSecurityContext(contextHandle, contextService, contextObjectProperties);
    saveCDIContext(contextHandle, contextService, contextObjectProperties);
    contextHandle.setUseTransactionOfExecutionThread(
        isUseTransactionOfExecutionThread(contextObjectProperties));
    return contextHandle;
  }

  @Override
  public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
    shouldBeTrue(contextHandle instanceof ContextHandleImpl);
    ContextHandleImpl preContextHandle = (ContextHandleImpl) contextHandle;
    ContextHandleImpl resetContextHandle = new ContextHandleImpl();
    setupApplication(preContextHandle, resetContextHandle);
    setupSecurityContext(preContextHandle, resetContextHandle);
    setupCDIContext(preContextHandle, resetContextHandle);
    // FIXME the concurrency-ri doesn't handle additional information when executing task.
    // setupTransaction(preContextHandle, resetContextHandle);
    return resetContextHandle;
  }

  protected String getTransactionExecutionProperty(Map<String, String> executionProperties) {
    if (executionProperties != null && executionProperties.get(ManagedTask.TRANSACTION) != null) {
      return executionProperties.get(ManagedTask.TRANSACTION);
    }
    return ManagedTask.SUSPEND;
  }

  protected boolean isUseTransactionOfExecutionThread(Map<String, String> executionProperties) {
    return ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD
        .equals(getTransactionExecutionProperty(executionProperties));
  }

  protected void resetApplicationContext(ContextHandleImpl contextHandle) {
    logger.fine(() -> "Reset application context if necessary.");
    if (contextInfos.contains(ContextInfo.APPLICATION)
        && contextHandle.getContextClassLoader() != null) {
      logger.fine(() -> format("Reset class loader %s", contextHandle.getContextClassLoader()));
      final ClassLoader classLoaderToSet = contextHandle.getContextClassLoader();
      final Thread currentThread = Thread.currentThread();
      if (classLoaderToSet != currentThread.getContextClassLoader()) {
        currentThread.setContextClassLoader(classLoaderToSet);
      }
    }
  }

  protected void resetCDIContext(ContextHandleImpl contextHandle) {
    logger.fine(() -> "Reset CDI context if necessary.");
    if (contextInfos.contains(ContextInfo.CDI) && contextHandle.getCDIContextRestorer() != null) {
      try {
        contextHandle.getCDIContextRestorer().restore();
      } catch (Exception ex) {
        logger.log(Level.SEVERE, ex, () -> "Reset CDI context occurred error!");
      } finally {
        contextHandle.setCDIContextRestorer(null);
      }
    }
  }

  protected void resetSecurityContext(ContextHandleImpl contextHandle) {
    logger.fine(() -> "Reset Security context if necessary.");
    if (contextInfos.contains(ContextInfo.SECURITY)) {
      SecurityContexts.setCurrent(contextHandle.getSecurityContext());
    }
  }

  protected void resetTransaction(ContextHandleImpl contextHandle) {
    logger.fine(() -> "Reset transaction if necessary.");
    if (!contextHandle.isUseTransactionOfExecutionThread()) {
      TransactionManager tm = tryResolve(TransactionManager.class);
      try {
        if (tm != null) {
          tm.resume(contextHandle.getTransaction());
          logger.fine(() -> "Resume current transaction.");
        }
      } catch (SystemException | InvalidTransactionException | IllegalStateException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  protected void saveApplicationContext(ContextHandleImpl contextHandle,
      ContextService contextService, Map<String, String> contextObjectProperties) {
    logger.fine(() -> "Save application context if necessary.");
    if (contextInfos.contains(ContextInfo.APPLICATION)) {
      final ClassLoader contextClassloader;
      if (Thread.currentThread().getContextClassLoader() != null) {
        contextClassloader = Thread.currentThread().getContextClassLoader();
      } else {
        contextClassloader = ClassLoader.getSystemClassLoader();
      }
      logger.fine(() -> format("Save class loader %s", contextClassloader));
      contextHandle.setContextClassLoader(contextClassloader);
    }
  }

  protected void saveCDIContext(ContextHandleImpl contextHandle, ContextService contextService,
      Map<String, String> contextObjectProperties) {
    logger.fine(() -> "Save CDI context if necessary.");
    if (contextInfos.contains(ContextInfo.CDI)) {
      contextHandle
          .setCDIContextInstaller(new ContextInstaller(true, tryResolve(WeldManager.class)));
    }
  }

  protected void saveSecurityContext(ContextHandleImpl contextHandle, ContextService contextService,
      Map<String, String> contextObjectProperties) {
    logger.fine(() -> "Save security context if necessary.");
    if (contextInfos.contains(ContextInfo.SECURITY)) {
      contextHandle.setSecurityContext(SecurityContexts.getCurrent());
    }
  }

  protected void setupApplication(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    logger.fine(() -> "Setup application context if necessary.");
    if (contextInfos.contains(ContextInfo.APPLICATION)
        && preContextHandle.getContextClassLoader() != null) {
      final ClassLoader classLoaderToSet = preContextHandle.getContextClassLoader();
      final Thread currentThread = Thread.currentThread();
      final ClassLoader originalClassLoader = currentThread.getContextClassLoader();
      if (classLoaderToSet != originalClassLoader) {
        currentThread.setContextClassLoader(classLoaderToSet);
      }
      logger.fine(() -> format("Setup class loader %s", classLoaderToSet));
      resetContextHandle.setContextClassLoader(originalClassLoader);
    }
  }

  protected void setupCDIContext(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    logger.fine(() -> "Setup CDI context if necessary.");
    if (contextInfos.contains(ContextInfo.CDI)
        && preContextHandle.getCDIContextInstaller() != null) {
      resetContextHandle.setCDIContextRestorer(preContextHandle.getCDIContextInstaller().install());
    }
  }

  protected void setupSecurityContext(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    logger.fine(() -> "Setup Security context if necessary.");
    if (contextInfos.contains(ContextInfo.SECURITY)) {
      resetContextHandle.setSecurityContext(SecurityContexts.getCurrent());
      SecurityContexts.setCurrent(preContextHandle.getSecurityContext());
    }
  }

  protected void setupTransaction(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    logger.fine(() -> "Setup transaction context if necessary.");
    if (!preContextHandle.isUseTransactionOfExecutionThread()) {
      TransactionManager tm = tryResolve(TransactionManager.class);
      try {
        if (tm != null) {
          resetContextHandle.setUseTransactionOfExecutionThread(false);
          resetContextHandle.setTransaction(tm.suspend());
          logger.fine(() -> "Suspend current transaction.");
        }
      } catch (SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}

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
package org.corant.suites.concurrency.provider;

import static org.corant.context.Instances.tryResolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Sets.immutableSetOf;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.inject.Instance;
import org.corant.context.Contexts.ContextInstaller;
import org.corant.context.Instances;
import org.corant.context.SecurityContext.SecurityContextService;
import org.corant.suites.concurrency.ContextServiceConfig.ContextInfo;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午8:44:55
 *
 */
public class ContextSetupProviderImpl implements ContextSetupProvider {

  private static final long serialVersionUID = -5397394660587586147L;

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
    saveCDIContext(contextHandle, contextService, contextObjectProperties);
    saveSecurityContext(contextHandle, contextService, contextObjectProperties);
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
    return resetContextHandle;
  }

  protected void resetApplicationContext(ContextHandleImpl contextHandle) {
    if (contextInfos.contains(ContextInfo.APPLICATION)
        && contextHandle.getContextClassLoader() != null) {
      final ClassLoader classLoaderToSet = contextHandle.getContextClassLoader();
      final Thread currentThread = Thread.currentThread();
      if (classLoaderToSet != currentThread.getContextClassLoader()) {
        if (System.getSecurityManager() == null) {
          currentThread.setContextClassLoader(classLoaderToSet);
        } else {
          AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            currentThread.setContextClassLoader(classLoaderToSet);
            return null;
          });
        }
      }
    }
  }

  protected void resetCDIContext(ContextHandleImpl contextHandle) {
    if (contextInfos.contains(ContextInfo.CDI) && contextHandle.getCDIContextRestorer() != null) {
      contextHandle.getCDIContextRestorer().restore();
    }
  }

  protected void resetSecurityContext(ContextHandleImpl contextHandle) {
    if (contextInfos.contains(ContextInfo.SECURITY)) {
      contextHandle.setSecurityContext(contextHandle.getSecurityContext());
    }
  }

  protected void saveApplicationContext(ContextHandleImpl contextHandle,
      ContextService contextService, Map<String, String> contextObjectProperties) {
    if (contextInfos.contains(ContextInfo.APPLICATION)) {
      ClassLoader contextClassloader = null;
      if (Thread.currentThread().getContextClassLoader() != null) {
        contextClassloader = Thread.currentThread().getContextClassLoader();
      } else {
        contextClassloader = ClassLoader.getSystemClassLoader();
      }
      contextHandle.setContextClassLoader(contextClassloader);
    }
  }

  protected void saveCDIContext(ContextHandleImpl contextHandle, ContextService contextService,
      Map<String, String> contextObjectProperties) {
    if (contextInfos.contains(ContextInfo.CDI)) {
      contextHandle
          .setCDIContextInstaller(new ContextInstaller(true, tryResolve(WeldManager.class)));
    }
  }

  protected void saveSecurityContext(ContextHandleImpl contextHandle, ContextService contextService,
      Map<String, String> contextObjectProperties) {
    Instance<SecurityContextService> scp = Instances.select(SecurityContextService.class);
    if (contextInfos.contains(ContextInfo.SECURITY) && scp.isResolvable()) {
      contextHandle.setSecurityContext(scp.get().get());
    }
  }

  protected void setupApplication(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    if (contextInfos.contains(ContextInfo.APPLICATION)
        && preContextHandle.getContextClassLoader() != null) {
      final ClassLoader classLoaderToSet = preContextHandle.getContextClassLoader();
      final Thread currentThread = Thread.currentThread();
      final ClassLoader originalClassLoader = currentThread.getContextClassLoader();
      if (classLoaderToSet != originalClassLoader) {
        if (System.getSecurityManager() == null) {
          currentThread.setContextClassLoader(classLoaderToSet);
        } else {
          AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            currentThread.setContextClassLoader(classLoaderToSet);
            return null;
          });
        }
      }
      resetContextHandle.setContextClassLoader(originalClassLoader);
    }
  }

  protected void setupCDIContext(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    if (contextInfos.contains(ContextInfo.CDI)
        && preContextHandle.getCDIContextInstaller() != null) {
      resetContextHandle.setCDIContextRestorer(preContextHandle.getCDIContextInstaller().install());
    }
  }

  protected void setupSecurityContext(ContextHandleImpl preContextHandle,
      ContextHandleImpl resetContextHandle) {
    Instance<SecurityContextService> scp = Instances.select(SecurityContextService.class);
    if (contextInfos.contains(ContextInfo.SECURITY) && scp.isResolvable()) {
      resetContextHandle.setSecurityContext(scp.get().get());
      scp.get().set(preContextHandle.getSecurityContext());
    }
  }
}

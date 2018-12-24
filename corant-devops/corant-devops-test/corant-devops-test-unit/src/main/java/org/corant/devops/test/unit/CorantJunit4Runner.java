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
package org.corant.devops.test.unit;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.ObjectUtils.isNotNull;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import org.corant.Corant;
import org.corant.devops.test.unit.web.EnableRandomWebServerPort;
import org.corant.shared.normal.Names.ConfigNames;
import org.junit.runners.model.Statement;

/**
 * corant-devops-test-unit
 *
 * @author bingo 上午11:26:46
 *
 */
public interface CorantJunit4Runner {

  static Logger logger = Logger.getLogger(CorantJUnit4ClassRunner.class.getName());
  static final ThreadLocal<Corant> CORANT_HOLDER = new ThreadLocal<>();
  static final ThreadLocal<Map<Class<?>, UnmanagedInstance<?>>> TESTOBJECT_HOLDER =
      ThreadLocal.withInitial(HashMap::new);
  static final boolean RUNNING_IN_ECLIPSE =
      isNotNull(tryAsClass("org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"));
  static final ThreadLocal<Boolean> ENABLE_RDM_WEB_SERVER_PORT =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  static boolean isEnableRandomWebServerPort() {
    return ENABLE_RDM_WEB_SERVER_PORT.get();
  }

  default Statement classBlockWithCorant(final Class<?> testClass,
      final Supplier<Statement> classBlock) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          if (CORANT_HOLDER.get() == null) {
            logger.fine(() -> "Create corant instance for junit test!");
            enchance(testClass);
            CORANT_HOLDER.set(new Corant(testClass));
            CORANT_HOLDER.get().start();
          }
          classBlock.get().evaluate();
        } catch (Throwable t) {
          t.printStackTrace();
          throw t;
        } finally {
          System.clearProperty(ConfigNames.CFG_PF_KEY);
          if (isCloseCorentWhenTestEnd()) {
            logger.fine(() -> "Clean unmanaged test instance from junit test!");
            if (TESTOBJECT_HOLDER.get() != null) {
              TESTOBJECT_HOLDER.get().values().forEach(umi -> umi.preDestroy().dispose());
              TESTOBJECT_HOLDER.get().clear();
              TESTOBJECT_HOLDER.remove();
            }
            logger.fine(() -> "Clean corant instance from junit test!");
            if (CORANT_HOLDER.get() != null) {
              CORANT_HOLDER.get().stop();
              CORANT_HOLDER.remove();
            }
          }
        }
      }
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  default Object createTestWithCorant(Class<?> clazz) throws Exception {
    if (TESTOBJECT_HOLDER.get() == null) {
      TESTOBJECT_HOLDER.set(new HashMap<>());
    }
    return TESTOBJECT_HOLDER.get().computeIfAbsent(clazz,
        (cls) -> new Unmanaged(cls).newInstance().produce().inject().postConstruct()).get();
  }

  default void enchance(final Class<?> testClass) {
    EnableRandomWebServerPort randomPort = testClass.getAnnotation(EnableRandomWebServerPort.class);
    if (randomPort != null) {
      ENABLE_RDM_WEB_SERVER_PORT.set(Boolean.TRUE);
    }
    RunProfile profile = testClass.getAnnotation(RunProfile.class);
    if (profile != null && isNotBlank(profile.value())) {
      System.setProperty(ConfigNames.CFG_PF_KEY, profile.value());
    }
  }

  boolean isCloseCorentWhenTestEnd();

  void setCloseCorentWhenTestEnd(boolean closeCorentWhenTestEnd);
}

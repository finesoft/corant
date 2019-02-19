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

import static org.corant.shared.normal.Names.ConfigNames.CFG_PF_KEY;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import org.corant.Corant;
import org.junit.runners.model.Statement;

/**
 * corant-devops-test-unit
 *
 * @author bingo 上午11:26:46
 *
 */
public interface CorantJunit4Runner {

  Logger logger = Logger.getLogger(CorantJunit4Runner.class.getName());
  ThreadLocal<Corant> corants = new ThreadLocal<>();
  ThreadLocal<Boolean> enableRdmWebPorts = ThreadLocal.withInitial(() -> Boolean.FALSE);
  ThreadLocal<String> profiles = new ThreadLocal<>();
  ThreadLocal<Boolean> autoDisposes = ThreadLocal.withInitial(() -> Boolean.TRUE);
  ThreadLocal<Map<String, String>> addCfgPros = ThreadLocal.withInitial(HashMap::new);
  ThreadLocal<Map<Class<?>, UnmanagedInstance<?>>> testObjects =
      ThreadLocal.withInitial(HashMap::new);

  default Statement classBlockWithCorant(final Class<?> testClass,
      final Supplier<Statement> classBlock) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          if (corants.get() == null) {
            Class<?> configClass = configTestClass(testClass);
            logger.fine(() -> "Create corant instance for junit test.");
            corants.set(new Corant(configClass));
            corants.get().start();
          }
          classBlock.get().evaluate();
        } catch (Throwable t) {
          t.printStackTrace();
          throw t;
        } finally {
          if (!isEmbedded()) {
            if (isNotBlank(profiles.get())) {
              System.clearProperty(CFG_PF_KEY);
            }
            if (autoDisposes.get()) {
              logger.fine(() -> "Clean unmanaged test instance from junit test.");
              if (testObjects.get() != null) {
                testObjects.get().values().forEach(umi -> umi.preDestroy().dispose());
                testObjects.get().clear();
                testObjects.remove();
              }
              logger.fine(() -> "Clean corant instance from junit test.");
              if (corants.get() != null) {
                corants.get().stop();
                corants.remove();
              }
            }
            autoDisposes.remove();
            addCfgPros.remove();
            profiles.remove();
            enableRdmWebPorts.remove();
          }
        }
      }
    };
  }

  default Class<?> configTestClass(final Class<?> testClass) {
    RunConfig rc = null;
    if (isEmbedded() || (rc = testClass.getAnnotation(RunConfig.class)) == null) {
      return testClass;
    }
    enableRdmWebPorts.set(rc.randomWebPort());
    if (isNotBlank(rc.profile())) {
      profiles.set(rc.profile());
      System.setProperty(CFG_PF_KEY, rc.profile());
    }
    if (rc.addiConfigProperties().length > 0) {
      for (AddiConfigProperty acp : rc.addiConfigProperties()) {
        addCfgPros.get().put(acp.name(), acp.value());
      }
    }
    autoDisposes.set(rc.autoDispose());
    return rc.configClass() == null ? testClass : rc.configClass();
  }

  default Object createTestWithCorant(Class<?> clazz) throws Exception {
    if (testObjects.get() == null) {
      testObjects.set(new HashMap<>());
    }
    return testObjects.get().computeIfAbsent(clazz,
        (cls) -> new Unmanaged<>(cls).newInstance().produce().inject().postConstruct()).get();
  }

  default boolean isEmbedded() {
    return false;
  }

  void setEmbedded(boolean embedded);
}

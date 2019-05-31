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

import static org.corant.kernel.normal.Names.ConfigNames.CFG_LOCATION_EXCLUDE_PATTERN;
import static org.corant.kernel.normal.Names.ConfigNames.CFG_PROFILE_KEY;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
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

  ThreadLocal<Corant> CORANTS = new ThreadLocal<>();
  ThreadLocal<Boolean> ENA_RDM_WEB_PORTS = ThreadLocal.withInitial(() -> Boolean.FALSE);
  ThreadLocal<String> PROFILES = new ThreadLocal<>();
  ThreadLocal<Boolean> AUTO_DISPOSES = ThreadLocal.withInitial(() -> Boolean.TRUE);
  ThreadLocal<Map<String, String>> ADDI_CFG_PROS = ThreadLocal.withInitial(HashMap::new);
  ThreadLocal<Map<Class<?>, UnmanagedInstance<?>>> TEST_OBJECTS =
      ThreadLocal.withInitial(HashMap::new);

  default Statement classBlockWithCorant(final Class<?> testClass,
      final Supplier<Statement> classBlock) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          if (CORANTS.get() == null) {
            Class<?> configClass = configTestClass(testClass);
            CORANTS.set(new Corant(configClass, testClass.getClassLoader()));
            CORANTS.get().start(new Class[0]);
          }
          classBlock.get().evaluate();
        } catch (Throwable t) {
          t.printStackTrace();
          throw t;
        } finally {
          if (!isEmbedded()) {
            if (isNotBlank(PROFILES.get())) {
              System.clearProperty(CFG_PROFILE_KEY);
            }
            System.clearProperty(CFG_LOCATION_EXCLUDE_PATTERN);
            if (AUTO_DISPOSES.get()) {
              if (TEST_OBJECTS.get() != null) {
                TEST_OBJECTS.get().values().forEach(umi -> umi.preDestroy().dispose());
                TEST_OBJECTS.get().clear();
                TEST_OBJECTS.remove();
              }
              if (CORANTS.get() != null) {
                CORANTS.get().stop();
                CORANTS.remove();
              }
            }
            AUTO_DISPOSES.remove();
            ADDI_CFG_PROS.remove();
            PROFILES.remove();
            ENA_RDM_WEB_PORTS.remove();
          }
        }
      }
    };
  }

  default Class<?> configTestClass(final Class<?> testClass) {
    System.setProperty(CFG_LOCATION_EXCLUDE_PATTERN, "**/target/classes/META-INF/*");
    RunConfig rc = null;
    if (isEmbedded() || (rc = testClass.getAnnotation(RunConfig.class)) == null) {
      return testClass;
    }
    ENA_RDM_WEB_PORTS.set(rc.randomWebPort());
    if (isNotBlank(rc.profile())) {
      PROFILES.set(rc.profile());
      System.setProperty(CFG_PROFILE_KEY, rc.profile());
    }
    if (isNotBlank(rc.excludeConfigUrlPattern())) {
      System.setProperty(CFG_LOCATION_EXCLUDE_PATTERN, rc.excludeConfigUrlPattern());
    } else {
      System.clearProperty(CFG_LOCATION_EXCLUDE_PATTERN);
    }
    if (rc.addiConfigProperties().length > 0) {
      for (AddiConfigProperty acp : rc.addiConfigProperties()) {
        ADDI_CFG_PROS.get().put(acp.name(), acp.value());
      }
    }
    AUTO_DISPOSES.set(rc.autoDispose());
    return rc.configClass() == null ? testClass : rc.configClass();
  }

  default Object createTestWithCorant(Class<?> clazz) throws Exception {
    if (TEST_OBJECTS.get() == null) {
      TEST_OBJECTS.set(new HashMap<>());
    }
    return TEST_OBJECTS.get().computeIfAbsent(clazz,
        (cls) -> new Unmanaged<>(cls).newInstance().produce().inject().postConstruct()).get();
  }

  default boolean isEmbedded() {
    return false;
  }

  void setEmbedded(boolean embedded);
}

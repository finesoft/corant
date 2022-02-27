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

import static org.corant.shared.normal.Names.ConfigNames.CFG_LOCATION_EXCLUDE_PATTERN;
import static org.corant.shared.normal.Names.ConfigNames.CFG_PROFILE_KEY;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.corant.Corant;
import org.corant.context.Beans;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Strings;
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
  ThreadLocal<String[]> ARGS = ThreadLocal.withInitial(() -> Strings.EMPTY_ARRAY);
  ThreadLocal<Class<?>[]> BEAN_CLASSES = new ThreadLocal<>();
  ThreadLocal<Boolean> AUTO_DISPOSES = ThreadLocal.withInitial(() -> Boolean.TRUE);
  ThreadLocal<Map<String, String>> ADDI_CFG_PROS = ThreadLocal.withInitial(HashMap::new);
  ThreadLocal<Map<Class<?>, Object>> TEST_OBJECTS = ThreadLocal.withInitial(HashMap::new);

  default Statement classBlockWithCorant(final Class<?> testClass, final Set<Class<?>> suiteClasses,
      final Supplier<Statement> classBlock) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          if (CORANTS.get() == null) {
            configTestClass(testClass, suiteClasses);
            CORANTS.set(new Corant(BEAN_CLASSES.get(), testClass.getClassLoader(), ARGS.get()));
            CORANTS.get().start(null);
          }
          classBlock.get().evaluate();
        } catch (Exception e) {
          e.printStackTrace();
          throw new CorantRuntimeException(e);
        } finally {
          if (!isEmbedded()) {
            if (isNotBlank(PROFILES.get())) {
              System.clearProperty(CFG_PROFILE_KEY);
            }
            System.clearProperty(CFG_LOCATION_EXCLUDE_PATTERN);
            if (AUTO_DISPOSES.get()) {
              if (TEST_OBJECTS.get() != null) {
                // TEST_OBJECTS.get().values().forEach(umi -> umi.preDestroy().dispose());
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
            BEAN_CLASSES.remove();
            ARGS.remove();
          }
        }
      }
    };
  }

  default void configTestClass(final Class<?> testClass, final Set<Class<?>> suitesClasses) {
    System.setProperty(CFG_LOCATION_EXCLUDE_PATTERN, "**/target/classes/META-INF/*");
    RunConfig rc;
    Set<Class<?>> classes = new LinkedHashSet<>();
    classes.add(testClass);
    if (isNotEmpty(suitesClasses)) {
      classes.addAll(suitesClasses);
    }
    if (!isEmbedded() && (rc = testClass.getAnnotation(RunConfig.class)) != null) {
      // BEAN_CLASSES.set(rc.beanClasses());
      if (rc.configClass() != null && !rc.configClass().equals(Object.class)) {
        classes.add(rc.configClass());
      }
      Collections.addAll(classes, rc.beanClasses());
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
      if (isNotEmpty(rc.arguments())) {
        ARGS.set(rc.arguments());
      }
      AUTO_DISPOSES.set(rc.autoDispose());
    }
    BEAN_CLASSES.set(classes.toArray(new Class<?>[classes.size()]));
  }

  default Object createTestWithCorant(Class<?> clazz) {
    if (TEST_OBJECTS.get() == null) {
      TEST_OBJECTS.set(new HashMap<>());
    }
    return TEST_OBJECTS.get().computeIfAbsent(clazz, Beans::resolve);
    /* cls -> new UnmanageableInstance<>(cls).produce().inject().postConstruct() ).get(); */
  }

  default boolean isEmbedded() {
    return false;
  }

  void setEmbedded(boolean embedded);
}

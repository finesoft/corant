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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;

/**
 * corant-devops-test-unit
 *
 * @author bingo 上午11:24:23
 *
 */
public class CorantJunit4Suite extends Suite implements CorantJunit4Runner {

  protected boolean embedded = false;
  protected final Set<Class<?>> managedClasses = new HashSet<>();

  /**
   * Called reflectively on classes annotated with @RunWith(Suite.class)
   *
   * @param klass the root class
   * @param builder builds runners for classes in the suite
   */
  public CorantJunit4Suite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
    super(klass, builder);
    resolveClasses(klass, true);
  }

  /**
   * @param builder
   * @param classes
   * @throws InitializationError
   */
  public CorantJunit4Suite(RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
    super(builder, classes);
    resolveClasses(classes);
  }

  /**
   * @param klass
   * @param managedClasses
   * @throws InitializationError
   */
  protected CorantJunit4Suite(Class<?> klass, Class<?>[] suiteClasses) throws InitializationError {
    super(klass, suiteClasses);
    resolveClasses(klass, true);
    resolveClasses(suiteClasses);
  }

  /**
   * @param klass
   * @param runners
   * @throws InitializationError
   */
  protected CorantJunit4Suite(Class<?> klass, List<Runner> runners) throws InitializationError {
    super(klass, runners);
    resolveClasses(klass, true);
  }

  /**
   * @param builder
   * @param klass
   * @param managedClasses
   * @throws InitializationError
   */
  protected CorantJunit4Suite(RunnerBuilder builder, Class<?> klass, Class<?>[] suiteClasses)
      throws InitializationError {
    super(builder, klass, suiteClasses);
    resolveClasses(klass, true);
    resolveClasses(suiteClasses);
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }

  @Override
  public void setEmbedded(boolean enableConfig) {
    embedded = enableConfig;
  }

  @Override
  protected Statement classBlock(RunNotifier notifier) {
    return classBlockWithCorant(getTestClass().getJavaClass(), managedClasses,
        () -> super.classBlock(notifier));
  }

  @Override
  protected List<Runner> getChildren() {
    List<Runner> runners = new ArrayList<>();
    super.getChildren().stream().forEach(runner -> {
      if (runner instanceof CorantJunit4Runner) {
        ((CorantJunit4Runner) runner).setEmbedded(true);
      }
      runners.add(runner);
    });
    return Collections.unmodifiableList(runners);
  }

  void resolveClasses(Class<?> clazz, boolean root) {
    if (clazz != null) {
      if (!root && !managedClasses.contains(clazz)) {
        managedClasses.add(clazz);
      }
      SuiteClasses annedClasses = clazz.getAnnotation(SuiteClasses.class);
      if (annedClasses != null) {
        for (Class<?> cls : annedClasses.value()) {
          resolveClasses(cls, false);
        }
      }
    }
  }

  void resolveClasses(Class<?>[] suiteClasses) {
    if (suiteClasses != null) {
      for (Class<?> cls : suiteClasses) {
        resolveClasses(cls, false);
      }
    }
  }

}

package org.corant.devops.test.unit;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

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

/**
 * corant-devops-test-unit
 *
 * @author bingo 下午5:42:55
 *
 */
public class CorantJUnit4ClassRunner extends BlockJUnit4ClassRunner implements CorantJunit4Runner {

  protected boolean closeCorentWhenTestEnd = true;

  /**
   * @param klass
   * @throws InitializationError
   */
  public CorantJUnit4ClassRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  public boolean isCloseCorentWhenTestEnd() {
    return closeCorentWhenTestEnd;
  }

  @Override
  public void setCloseCorentWhenTestEnd(boolean closeCorentWhenTestEnd) {
    this.closeCorentWhenTestEnd = closeCorentWhenTestEnd;
  }


  @Override
  protected Statement classBlock(RunNotifier notifier) {
    return classBlockWithCorant(getTestClass().getJavaClass(),
        () -> CorantJUnit4ClassRunner.super.classBlock(notifier));
  }


  @Override
  protected synchronized Object createTest() throws Exception {
    return createTestWithCorant(getTestClass().getJavaClass());
  }
}

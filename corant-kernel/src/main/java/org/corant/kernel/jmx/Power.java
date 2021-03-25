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
package org.corant.kernel.jmx;

import java.util.Arrays;
import org.corant.Corant;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings;

/**
 * corant-kernel
 *
 * @author bingo 下午3:30:49
 *
 */
public class Power implements PowerMBean {

  private Class<?>[] beanClasses = Classes.EMPTY_ARRAY;
  private String[] arguments = Strings.EMPTY_ARRAY;

  public Power(Class<?>[] beanClasses, String[] arguments) {
    if (beanClasses != null) {
      this.beanClasses = Arrays.copyOf(beanClasses, beanClasses.length);
    }
    if (arguments != null) {
      this.arguments = Arrays.copyOf(arguments, arguments.length);
    }
  }

  @Override
  public synchronized boolean isRunning() {
    try {
      return Corant.current() != null && Corant.current().isRunning();
    } catch (Exception t) {
      throw new IllegalStateException("Can't check corant running! please check logging.");
    }
  }

  @Override
  public synchronized void start() {
    try {
      if (Corant.current() == null) {
        Corant.startup(beanClasses, arguments);
      } else if (!Corant.current().isRunning()) {
        Corant.current().start(Functions.emptyConsumer());
      }
    } catch (Exception t) {
      throw new IllegalStateException("Can't start corant! please check logging.");
    }
  }

  @Override
  public synchronized void stop() {
    try {
      if (Corant.current() != null && Corant.current().isRunning()) {
        Corant.current().stop();
      }
    } catch (Exception t) {
      throw new IllegalStateException("Can't stop corant! please check logging.");
    }
  }

}

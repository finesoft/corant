/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jta.shared;

import java.util.function.IntConsumer;
import javax.transaction.Synchronization;

/**
 * corant-modules-jta-shared
 *
 * @author bingo 上午10:12:56
 *
 */
public class SynchronizationAdapter implements Synchronization {

  public static Synchronization afterCompletion(IntConsumer consumer) {
    return new SynchronizationAdapter() {
      @Override
      public void afterCompletion(int status) {
        if (consumer != null) {
          consumer.accept(status);
        }
      }
    };
  }

  public static Synchronization afterCompletion(Runnable runner) {
    return new SynchronizationAdapter() {
      @Override
      public void afterCompletion(int status) {
        if (runner != null) {
          runner.run();
        }
      }
    };
  }

  public static Synchronization beforeCompletion(Runnable runner) {
    return new SynchronizationAdapter() {
      @Override
      public void beforeCompletion() {
        if (runner != null) {
          runner.run();
        }
      }
    };
  }

  @Override
  public void afterCompletion(int status) {

  }

  @Override
  public void beforeCompletion() {

  }

}

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
package org.corant.kernel.boot;

import org.corant.Corant;

/**
 * corant-kernel
 *
 * @author bingo 下午3:32:02
 *
 */
public class DirectRunner {

  private DirectRunner() {}

  public static void main(String... args) {
    new DirectRunner().run(args);
  }

  public void run(String... args) {
    Corant.run(new Class[0], args);
    synchronized (this) {
      while (true) {
        try {
          this.wait(2000L);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}

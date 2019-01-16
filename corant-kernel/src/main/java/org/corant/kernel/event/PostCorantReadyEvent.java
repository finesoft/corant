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
package org.corant.kernel.event;

import java.util.Arrays;

/**
 * corant-kernel
 *
 * <pre>
 * CDI container started, embedded server started, ready for service.
 * </pre>
 *
 * @author bingo 上午10:59:20
 *
 */
public class PostCorantReadyEvent implements CorantLifecycleEvent {
  private final String[] args;

  /**
   * @param args
   */
  public PostCorantReadyEvent(String[] args) {
    super();
    this.args = Arrays.copyOf(args, args.length);
  }

  /**
   *
   * @return the args
   */
  public String[] getArgs() {
    return Arrays.copyOf(args, args.length);
  }
}

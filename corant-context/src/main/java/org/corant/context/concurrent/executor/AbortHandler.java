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
package org.corant.context.concurrent.executor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * corant-context
 *
 * @author bingo 下午8:27:44
 *
 */
public class AbortHandler implements RejectedExecutionHandler {

  static final Logger logger = Logger.getLogger(AbortHandler.class.getName());

  final String name;

  public AbortHandler(String name) {
    this.name = name;
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    String msg = "The task " + r.toString() + " was rejected from the executor "
        + executor.toString() + " in the executor service " + name;
    logger.warning(msg);
    throw new RejectedExecutionException(msg);
  }

}

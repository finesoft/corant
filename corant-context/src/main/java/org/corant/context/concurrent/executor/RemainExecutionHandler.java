/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import java.util.List;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-context
 *
 * @author bingo 23:34:25
 */
public interface RemainExecutionHandler extends Sortable {

  /**
   * Handle the remain executions on managed executor service shutdown, all executions are retrieved
   * from {@code ExecutorService.shutdownNow()}.
   *
   * @param scheduled indicates whether the executor service is scheduled executor service
   * @param executorServiceName the managed executor service name
   * @param runnable the remain execution runnable
   *
   * @see java.util.concurrent.ExecutorService#shutdownNow()
   */
  void handle(boolean scheduled, String executorServiceName, List<Runnable> runnable);

}

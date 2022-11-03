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
package org.corant.modules.webserver.jetty;

import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Objects.min;
import java.util.function.Function;
import org.corant.modules.webserver.shared.WebServerConfig;
import org.corant.shared.util.Systems;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * corant-modules-webserver-jetty
 *
 * @author bingo 下午2:31:33
 *
 */
@FunctionalInterface
public interface JettyWebServerThreadPoolProvider extends Function<WebServerConfig, ThreadPool> {

  JettyWebServerThreadPoolProvider DEFAULT = new JettyWebServerThreadPoolProvider() {

    @Override
    public ThreadPool apply(WebServerConfig config) {
      final int maxThreads = config.getWorkThreads();
      final int minThreads = min(config.getWorkThreads(), Systems.getCPUs());
      final int queueCapacity = max(minThreads, 8);
      return new QueuedThreadPool(maxThreads, minThreads, 60000,
          new BlockingArrayQueue<>(queueCapacity, queueCapacity));
    }

  };

}

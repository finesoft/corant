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
package org.corant.suites.webserver.undertow;

import java.util.function.BiConsumer;
import org.corant.kernel.config.ComparableConfigurator;
import org.xnio.Option;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * corant-suites-webserver-undertow
 *
 * @author bingo 上午10:32:02
 *
 */
public interface UndertowWebServerConfigurator extends ComparableConfigurator {

  default void configureDeployment(DeploymentInfo deploymentInfo) {}

  default <T> void configureServerOptions(BiConsumer<Option<T>, T> consumer) {}

  default <T> void configureSocketOptions(BiConsumer<Option<T>, T> consumer) {}

  default <T> void configureWorkOptions(BiConsumer<Option<T>, T> consumer) {}

}

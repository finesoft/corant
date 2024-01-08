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
package org.corant.modules.javafx.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import javafx.application.Application.Parameters;

/**
 * corant-modules-javafx-cdi
 *
 * <p>
 * A application parameters producer, use to make application parameters injectable.
 *
 * @author bingo 上午12:21:56
 */
@Singleton
public class CorantApplicationParametersFactory {

  protected volatile Parameters parameters;

  public @Produces Parameters getParameters() {
    return parameters;
  }

  protected void setParameters(Parameters p) {
    parameters = p;
  }
}

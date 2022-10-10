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

import org.corant.Corant;
import org.corant.shared.util.Annotations;
import javafx.application.Application.Parameters;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午5:40:17
 *
 */
public class CorantJavaFX {

  public static void startCorant(Parameters parameters) {
    if (Corant.current() == null) {
      synchronized (Corant.class) {
        if (Corant.current() == null) {
          Corant.call(false, CorantApplicationParametersFactory.class, Annotations.EMPTY_ARRAY,
              parameters.getRaw().toArray(String[]::new)).setParameters(parameters);
        }
      }
    }
    if (!Corant.current().isRunning()) {
      Corant.current().start(null);
    }
  }

  public static void stopCorant() {
    if (Corant.current() != null) {
      synchronized (Corant.class) {
        if (Corant.current() != null) {
          Corant.shutdown();
        }
      }
    }
  }
}

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

import static org.corant.context.Beans.resolve;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Singleton;
import org.corant.Corant;
import javafx.application.Application;
import javafx.application.Application.Parameters;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午5:40:17
 *
 */
public class CorantJavaFX {

  public static void startCorant(Application application) {
    if (Corant.current() == null) {
      synchronized (Corant.class) {
        if (Corant.current() == null) {
          final Parameters parameters = application.getParameters();
          Corant.startup(sc -> sc.addExtensions(new ApplicationExtension(application)),
              parameters.getRaw().toArray(String[]::new));
          resolve(CorantApplicationParametersFactory.class).setParameters(parameters);
        }
      }
    }
    if (!Corant.current().isRunning()) {
      Corant.current().start(sc -> sc.addExtensions(new ApplicationExtension(application)));
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

  static class ApplicationExtension implements Extension {

    final Application application;

    ApplicationExtension(Application application) {
      this.application = application;
    }

    void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
      event.addBean().addType(application.getClass()).scope(Singleton.class)
          .addQualifier(Default.Literal.INSTANCE).addQualifier(Any.Literal.INSTANCE)
          .produceWith(obj -> application);
    }

  }
}

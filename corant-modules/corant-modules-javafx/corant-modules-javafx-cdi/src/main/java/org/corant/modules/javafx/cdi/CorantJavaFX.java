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
 * <p>
 * Corant JavaFX bootstrap for JavaFX applications integrated with CDI. Generally, when
 * {@link Application#init()} and {@link Application#stop()}, the CDI container is started and
 * closed accordingly. The command line arguments passed to the application can be passed to Corant
 * correspondingly, and make the application instance injectable.
 * <p>
 * NOTE: Each process has one and only one corant instance, it can be startup and shutdown mulity
 * times, the command line arguments only passed to it only first startup.
 *
 * @author bingo 下午5:40:17
 *
 */
public class CorantJavaFX {

  /**
   * Start corant with given application, if corant has already been started, this method does
   * nothing.
   *
   * @param application the JavaFX application
   */
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
    } else {
      synchronized (Corant.class) {
        if (!Corant.current().isRunning()) {
          Corant.current().start(sc -> sc.addExtensions(new ApplicationExtension(application)));
        }
      }
    }
  }

  /**
   * Stop corant, if corant has already been stopped or corant instance doesn't exist, this
   * methoddoes nothing.
   */
  public static void stopCorant() {
    if (Corant.current() != null) {
      synchronized (Corant.class) {
        if (Corant.current() != null) {
          Corant.shutdown();
        }
      }
    }
  }

  /**
   * corant-modules-javafx-cdi
   *
   * <p>
   * An application instance for injection.
   *
   * @author bingo 下午5:50:34
   *
   */
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

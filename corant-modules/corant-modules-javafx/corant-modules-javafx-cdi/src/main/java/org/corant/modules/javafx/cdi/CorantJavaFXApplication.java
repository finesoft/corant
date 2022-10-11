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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.context.CDIs;
import org.corant.modules.javafx.cdi.CorantFXML.CorantFXMLLiteral;
import javafx.application.Application;
import javafx.application.Preloader.ErrorNotification;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 上午12:31:43
 *
 */
public class CorantJavaFXApplication extends Application {

  protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

  @Override
  public final void init() throws Exception {
    initCorant();
    super.init();
    doInit();
  }

  @Override
  public final void start(Stage primaryStage) throws Exception {
    CDIs.fireEvent(primaryStage, StartupScene.INSTANCE);
    doStart(primaryStage);
  }

  @Override
  public final void stop() throws Exception {
    try {
      super.stop();
      doStop();
    } finally {
      stopCorant();
    }
  }

  protected void doInit() throws Exception {}

  protected void doInitCorant() throws Exception {
    notifyPreloader(new CorantInitializationNotification("Initialize Corant...", this));
    CorantJavaFX.startCorant(getParameters());
    notifyPreloader(new CorantInitializationNotification("Corant initialized", this));
  }

  protected void doStart(Stage primaryStage) throws Exception {}

  protected void doStop() throws Exception {}

  protected void initCorant() throws Exception {
    try {
      doInitCorant();
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Initialize Corant occurred error!", ex);
      notifyPreloader(new ErrorNotification(null, "Initialize Corant occurred error!", ex));
      throw ex;
    }
  }

  protected FXMLLoader resolveFXMLLoader() {
    return resolveFXMLLoader(null, null, null);
  }

  protected FXMLLoader resolveFXMLLoader(String bundle) {
    return resolveFXMLLoader(bundle, null, null);
  }

  protected FXMLLoader resolveFXMLLoader(String bundle, String url) {
    return resolveFXMLLoader(bundle, url, null);
  }

  protected FXMLLoader resolveFXMLLoader(String bundle, String url, String charset) {
    return resolve(FXMLLoader.class, CorantFXMLLiteral.of(bundle, url, charset));
  }

  protected void stopCorant() {
    CorantJavaFX.stopCorant();
  }

}

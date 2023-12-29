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

import javafx.application.Preloader;
import javafx.stage.Stage;

/**
 * corant-modules-javafx-cdi
 *
 * <p>
 * Preloader flow:
 *
 * <pre>
 * 1. Preloader constructor called, thread: JavaFX Application Thread
 * 2. Preloader#init (could be used to initialize preloader view), thread: JavaFX-Launcher
 * 3. Preloader#start (showing preloader stage), thread: JavaFX Application Thread
 * 4. BEFORE_LOAD
 * 5. Application constructor called, thread: JavaFX Application Thread
 * 6. BEFORE_INIT
 * 7. Application#init (doing some heavy lifting), thread: JavaFX-Launcher
 * 8. BEFORE_START
 * 9. Application#start (initialize and show primary application stage), thread: JavaFX Application Thread
 * </pre>
 *
 * @author bingo 下午11:51:55
 */
public class CorantJavaFXPreloader extends Preloader {

  protected volatile Stage preloaderStage;

  @Override
  public boolean handleErrorNotification(ErrorNotification info) {
    return super.handleErrorNotification(info);
  }

  @Override
  public void handleStateChangeNotification(StateChangeNotification info) {
    final Stage usedStage = preloaderStage;
    if (info.getType() == StateChangeNotification.Type.BEFORE_START && usedStage != null) {
      usedStage.hide();
    }
  }

  @Override
  public final void start(Stage preloaderStage) throws Exception {
    this.preloaderStage = preloaderStage;
    doStart(this.preloaderStage);
  }

  protected void doStart(Stage preloaderStage) {}
}

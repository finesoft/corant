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

import static org.corant.shared.util.Strings.EMPTY;
import javafx.application.Application;
import javafx.application.Preloader.PreloaderNotification;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午7:18:33
 *
 */
public class CorantInitializationNotification implements PreloaderNotification {

  private final Double progress;
  private final String details;
  private final Application application;

  public CorantInitializationNotification(Double progress) {
    this(progress, EMPTY, null);
  }

  public CorantInitializationNotification(Double progress, Application application) {
    this(progress, EMPTY, application);
  }

  public CorantInitializationNotification(Double progress, String details) {
    this(progress, details, null);
  }

  public CorantInitializationNotification(Double progress, String details,
      Application application) {
    this.progress = progress;
    this.details = details;
    this.application = application;
  }

  public CorantInitializationNotification(String details) {
    this(null, details, null);
  }

  public CorantInitializationNotification(String details, Application application) {
    this(null, details, application);
  }

  public Application getApplication() {
    return application;
  }

  public String getDetails() {
    return details;
  }

  public Double getProgress() {
    return progress;
  }

}

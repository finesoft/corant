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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ResourceBundle;
import org.corant.context.Beans;
import javafx.fxml.FXMLLoader;
import javafx.util.BuilderFactory;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午4:30:32
 *
 */
public class FXMLLoaders {

  public static <T> T load(URL location) throws IOException {
    return FXMLLoader.load(location, null, null, Beans::resolve);
  }

  public static <T> T load(URL location, ResourceBundle resources) throws IOException {
    return FXMLLoader.load(location, resources, null, Beans::resolve);
  }

  public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory)
      throws IOException {
    return FXMLLoader.load(location, resources, builderFactory, Beans::resolve);
  }

  public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory,
      Charset charset) throws IOException {
    return FXMLLoader.load(location, resources, builderFactory, Beans::resolve, charset);
  }

}

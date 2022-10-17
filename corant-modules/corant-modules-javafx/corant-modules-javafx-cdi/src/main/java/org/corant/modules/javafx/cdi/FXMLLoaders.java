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
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ResourceBundle;
import org.corant.context.Beans;
import org.corant.shared.resource.URLResource;
import org.corant.shared.util.Resources;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.BuilderFactory;

/**
 * corant-modules-javafx-cdi
 * <p>
 * A CDI related FXMLLoader utility class.
 *
 * @author bingo 下午4:30:32
 *
 */
public class FXMLLoaders {

  private FXMLLoaders() {}

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param pathOrExpression the resource path or path expression
   * @return the loaded object hierarchy
   * @throws IOException if an error occurs during loading
   *
   * @see Resources#from(String)
   */
  public static <T> T load(String pathOrExp) throws IOException {
    Optional<URLResource> resource = Resources.from(pathOrExp).findAny();
    if (resource.isPresent()) {
      try (InputStream is = resource.get().openInputStream()) {
        return new FXMLLoader(null, null, new JavaFXBuilderFactory(), Beans::resolve,
            StandardCharsets.UTF_8).load(is);
      }
    }
    return null;
  }

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param location the location used to resolve relative path attribute values
   *
   * @throws IOException if an error occurs during loading
   * @return the loaded object hierarchy
   */
  public static <T> T load(URL location) throws IOException {
    return FXMLLoader.load(location, null, null, Beans::resolve);
  }

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param location the location used to resolve relative path attribute values
   * @param resources the resources used to resolve resource key attribute values
   *
   * @throws IOException if an error occurs during loading
   * @return the loaded object hierarchy
   */
  public static <T> T load(URL location, ResourceBundle resources) throws IOException {
    return FXMLLoader.load(location, resources, null, Beans::resolve);
  }

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param location the location used to resolve relative path attribute values
   * @param resources the resources used to resolve resource key attribute values
   * @param builderFactory the builder factory used to load the document
   *
   * @throws IOException if an error occurs during loading
   * @return the loaded object hierarchy
   */
  public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory)
      throws IOException {
    return FXMLLoader.load(location, resources, builderFactory, Beans::resolve);
  }

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param location the location used to resolve relative path attribute values
   * @param resources the resources used to resolve resource key attribute values
   * @param builderFactory the builder factory used when loading the document
   * @param charset the character set used when loading the document
   *
   * @throws IOException if an error occurs during loading
   * @return the loaded object hierarchy
   *
   * @since JavaFX 2.1
   */
  public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory,
      Charset charset) throws IOException {
    return FXMLLoader.load(location, resources, builderFactory, Beans::resolve, charset);
  }

}

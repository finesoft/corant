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

import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import org.corant.context.Beans;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.ClassPathResourceLoader;
import org.corant.shared.resource.URLResource;
import org.corant.shared.util.Annotations;
import org.corant.shared.util.Resources;
import javafx.fxml.FXMLLoader;
import javafx.util.BuilderFactory;
import javafx.util.Callback;

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
   * Returns a simple FXMLLoader builder for build a FXMLLoader instance. The controller factory in
   * the document is taken over by CDI.
   */
  public static FXMLLoaderBuilder builder() {
    return new FXMLLoaderBuilder();
  }

  /**
   * Loads an object hierarchy from a FXML document. The controller factory in the document is taken
   * over by CDI.
   *
   * @param <T> the type of the root object
   * @param pathOrExp the resource path or path expression
   * @return the loaded object hierarchy
   * @throws IOException if an error occurs during loading
   *
   * @see Resources#from(String)
   */
  public static <T> T load(String pathOrExp) throws IOException {
    return builder().location(pathOrExp).build().load();
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
    return builder().location(location).build().load();
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
    return builder().location(location).resources(resources).build().load();
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
    return builder().location(location).resources(resources).builderFactory(builderFactory).build()
        .load();
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
    return builder().location(location).resources(resources).builderFactory(builderFactory)
        .charset(charset).build().load();
  }

  /**
   * Loads an object hierarchy from a FXML document with a relative path through a class and path.
   * The controller factory in the document is taken over by CDI.
   *
   * @param <T> the type of the root object
   * @param relative the relative class use to search the resource
   * @param path the resource path
   * @return the loaded object hierarchy
   * @throws IOException if an error occurs during loading
   *
   * @see Resources#fromRelativeClass(Class, String)
   * @see ClassPathResourceLoader#relative(Class, String)
   */
  public static <T> T loadRelative(Class<?> relative, String path) throws IOException {
    URLResource resource = Resources.fromRelativeClass(relative, path);
    if (resource != null) {
      return builder().location(resource.getURL()).build().load();
    }
    return null;
  }

  /**
   * corant-modules-javafx-cdi
   *
   * @author bingo 下午4:40:25
   *
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static class FXMLLoaderBuilder {
    URL location;
    ResourceBundle resources;
    BuilderFactory builderFactory;
    Charset charset = StandardCharsets.UTF_8;
    LinkedList<FXMLLoader> loaders;
    Annotation[] controllerQualifiers = Annotations.EMPTY_ARRAY;
    Map<Class<?>, Consumer> postControllerFactoryCalls = new LinkedHashMap<>();

    public FXMLLoader build() {
      final Callback<Class<?>, Object> controllerFactory = t -> {
        Object ctrl = Beans.resolve(t, controllerQualifiers);
        if (!postControllerFactoryCalls.isEmpty()) {
          postControllerFactoryCalls.forEach((c, s) -> {
            if (c.isInstance(ctrl)) {
              s.accept(ctrl);
            }
          });
        }
        return ctrl;
      };
      return new FXMLLoader(location, resources, builderFactory, controllerFactory, charset,
          defaultObject(loaders, LinkedList::new));
    }

    public FXMLLoaderBuilder builderFactory(BuilderFactory builderFactory) {
      this.builderFactory = builderFactory;
      return this;
    }

    public FXMLLoaderBuilder charset(Charset charset) {
      this.charset = charset;
      return this;
    }

    public FXMLLoaderBuilder controllerQualifiers(Annotation... controllerQualifiers) {
      if (controllerQualifiers.length > 0) {
        this.controllerQualifiers =
            Arrays.copyOf(controllerQualifiers, controllerQualifiers.length);
      }
      return this;
    }

    public FXMLLoaderBuilder loaders(LinkedList<FXMLLoader> loaders) {
      this.loaders = loaders;
      return this;
    }

    public FXMLLoaderBuilder location(String pathOrExp) {
      try {
        Optional<URLResource> res = Resources.from(pathOrExp).findAny();
        location = res.map(URLResource::getURL).orElse(null);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
      return this;
    }

    public FXMLLoaderBuilder location(URL location) {
      this.location = location;
      return this;
    }

    /**
     * Attaches a controller factory callback to the builder that will be invoked after CDI resolves
     * the controller instance and before FXML initializes it.
     *
     * @param <C> the controller type that consumed by the callback
     * @param ctrlClass the controller class that consumed by the callback
     * @param postControllerFactoryCall the callback that will be invoked after CDI resolves the
     *        controller instance and before FXML initializes it.
     * @return the builder
     */
    public <C> FXMLLoaderBuilder putPostControllerFactoryCall(Class<C> ctrlClass,
        Consumer<C> postControllerFactoryCall) {
      postControllerFactoryCalls.put(ctrlClass, postControllerFactoryCall);
      return this;
    }

    /**
     * Removes previously placed controller factory callback based on the specified controller type.
     *
     * @param ctrlClass the controller class that consumed by the callback
     * @return the builder
     */
    public FXMLLoaderBuilder removePostControllerFactoryCall(Class<?> ctrlClass) {
      postControllerFactoryCalls.remove(ctrlClass);
      return this;
    }

    public FXMLLoaderBuilder resources(ResourceBundle resources) {
      this.resources = resources;
      return this;
    }
  }
}

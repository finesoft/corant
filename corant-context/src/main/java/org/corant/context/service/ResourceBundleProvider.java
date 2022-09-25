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
package org.corant.context.service;

import java.util.ResourceBundle;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.context.qualifier.Bundle;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午10:14:42
 *
 */
public class ResourceBundleProvider {

  @Produces
  @Dependent
  @Bundle("")
  ResourceBundle produce(final InjectionPoint injectionPoint) {
    final String bundleName = injectionPoint.getAnnotated().getAnnotation(Bundle.class).value();
    return ResourceBundle.getBundle(bundleName);
  }
}

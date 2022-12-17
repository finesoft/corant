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
package org.corant.modules.validation;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.corant.config.Configs;
import org.corant.modules.bundle.PropertyMessageSource;
import org.corant.modules.bundle.PropertyResourceBundle;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

/**
 * corant-modules-validation
 *
 * <p>
 * A simple class that scans all message resources according to URL expressions, and uses these
 * resources and their priorities to construct {@link ResourceBundle}.
 *
 * @see PropertyResourceBundle
 *
 * @author bingo 上午11:23:30
 *
 */
public class PrioritizedResourceBundleLocator implements ResourceBundleLocator {

  protected static final Map<Locale, PropertyResourceBundle> bundles = PropertyResourceBundle
      .getFoldedLocaleBundles(Configs.getValue(PropertyMessageSource.BUNDLE_PATHS_CFG_KEY,
          String.class, PropertyMessageSource.DEFAULT_BUNDLE_PATHS));

  @Override
  public ResourceBundle getResourceBundle(Locale locale) {
    return bundles.get(locale);
  }

}

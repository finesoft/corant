/*
 * Copyright (c) 2013-2018. BIN.CHEN
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
package org.corant.modules.bundle;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.corant.modules.bundle.PropertyResourceBundle.getFoldedLocaleBundles;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.split;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-bundle
 *
 * @author bingo 上午12:26:12
 *
 */
@ApplicationScoped
public class PropertyMessageSource implements MessageSource {

  public static final String BUNDLE_PATHS_CFG_KEY = "corant.bundle.message-file.paths";
  public static final String DEFAULT_BUNDLE_PATHS = "META-INF/**Messages_*.properties";
  protected final Map<Locale, PropertyResourceBundle> bundles = new ConcurrentHashMap<>();

  protected volatile boolean initialized = false;

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = BUNDLE_PATHS_CFG_KEY, defaultValue = DEFAULT_BUNDLE_PATHS)
  protected String bundleFilePaths;

  @Override
  public synchronized void close() throws Exception {
    initialized = false;
    bundles.clear();
    logger.fine(() -> "Close property message source, all bundles are cleared.");
  }

  public Map<Locale, PropertyResourceBundle> getBundles() {
    return unmodifiableMap(bundles);
  }

  @Override
  public Set<String> getKeys(Locale locale) {
    PropertyResourceBundle bundle = bundles.get(locale);
    if (bundle != null) {
      return unmodifiableSet(bundle.keySet());
    }
    return emptySet();
  }

  @Override
  public Set<Locale> getLocales() {
    return unmodifiableSet(bundles.keySet());
  }

  @Override
  public String getMessage(Locale locale, Object key) throws NoSuchMessageException {
    if (key == null) {
      throw new NoSuchMessageException("The message property key can't null");
    } else {
      load();
      Locale useLocale = defaultObject(locale, Locale::getDefault);
      PropertyResourceBundle bundle = bundles.get(useLocale);
      if (bundle == null || !bundle.containsKey(key.toString())) {
        throw new NoSuchMessageException("Can't find message for %s with locale %s.",
            key.toString(), useLocale.toString());
      }
      return bundle.getString(key.toString());
    }
  }

  @Override
  public String getMessage(Locale locale, Object key, Function<Locale, String> defaultMessage) {
    if (key == null) {
      return defaultMessage.apply(locale);
    } else {
      load();
      Locale useLocale = defaultObject(locale, Locale::getDefault);
      PropertyResourceBundle bundle = bundles.get(useLocale);
      if (bundle == null || !bundle.containsKey(key.toString())) {
        return defaultMessage.apply(useLocale);
      } else {
        String message = bundle.getString(key.toString());
        return defaultObject(message, () -> defaultMessage.apply(useLocale));
      }
    }
  }

  @Override
  public synchronized void refresh() {
    logger.info(() -> "Refresh property message bundles.");
    initialized = false;
    load();
  }

  protected boolean isInitialized() {
    return initialized;
  }

  protected void load() {
    if (!isInitialized()) {
      synchronized (this) {
        if (!isInitialized()) {
          try {
            bundles.clear();
            bundles.putAll(getFoldedLocaleBundles(split(bundleFilePaths, ",")));
            logger.info(() -> "All property message bundles are loaded.");
          } finally {
            initialized = true;
            CDI.current().getBeanManager().getEvent().fire(new MessageSourceRefreshedEvent(this));
          }
        }
      }
    }
  }

}

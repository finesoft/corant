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

import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Functions.emptyBiPredicate;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.split;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-bundle
 *
 * @author bingo 上午12:26:12
 *
 */
@ApplicationScoped
public class PropertyMessageSource implements MessageSource {

  protected final Map<Locale, PropertyResourceBundle> bundles = new ConcurrentHashMap<>();

  protected volatile boolean initialized = false;

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = "corant.bundle.message-file.paths",
      defaultValue = "META-INF/**Messages_*.properties")
  protected String bundleFilePaths;

  @Inject
  @Any
  protected Instance<MessageSourceFilter> filters;

  protected BiPredicate<String, String> filter = emptyBiPredicate(true);

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
      String message = bundle.getString(key.toString());
      if (!filter.test(key.toString(), message)) {
        throw new NoSuchMessageException("Can't find message for %s with locale %s.",
            key.toString(), useLocale.toString());
      }
      return message;
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
        if (!filter.test(key.toString(), message)) {
          message = null;
        }
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
            Set<String> paths = setOf(split(bundleFilePaths, ","));
            paths.stream().filter(Strings::isNotBlank)
                .flatMap(pkg -> PropertyResourceBundle.getBundles(pkg, filter).stream())
                .sorted(Sortable::reverseCompare).forEachOrdered(res -> {
                  Locale locale = res.getLocale();
                  PropertyResourceBundle bundle = bundles.get(locale);
                  if (bundle == null) {
                    bundles.put(locale, res);
                  } else {
                    res.setParent(bundle);
                    bundle = res;
                    bundles.put(locale, bundle);
                  }
                  logger.fine(() -> String.format("Found message resource from %s.", res.getUri()));
                });
            logger.info(() -> "All property message bundles are loaded.");
          } finally {
            initialized = true;
            CDI.current().getBeanManager().getEvent().fire(new MessageSourceRefreshedEvent(this));
          }
        }
      }
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    if (!filters.isUnsatisfied()) {
      filter = filters.stream().max(Sortable::compare).get();
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    try {
      close();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

}

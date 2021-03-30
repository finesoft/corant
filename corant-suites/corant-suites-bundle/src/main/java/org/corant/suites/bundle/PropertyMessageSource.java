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
package org.corant.suites.bundle;

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.split;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author bingo 上午12:26:12
 *
 */
@ApplicationScoped
public class PropertyMessageSource implements MessageSource {

  protected final Map<Locale, Map<String, MessageFormat>> holder = new ConcurrentHashMap<>(128);

  protected volatile boolean initialized = false;

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = "bundle.message-file.paths",
      defaultValue = "META-INF/**Messages_*.properties")
  protected String bundleFilePaths;

  @Override
  public String getMessage(Locale locale, Object key, Object[] args) throws NoSuchBundleException {
    load();
    if (key == null) {
      throw new NoSuchBundleException("The message property key can't null");
    } else {
      Locale useLocale = defaultObject(locale, Locale::getDefault);
      Map<String, MessageFormat> mfMap = holder.get(useLocale);
      if (mfMap == null) {
        throw new NoSuchBundleException("Can't find message for %s with locale %s.", key.toString(),
            useLocale.toString());
      } else {
        MessageFormat mf = mfMap.get(key);
        if (mf == null) {
          throw new NoSuchBundleException("Can't find message for %s with locale %s.",
              key.toString(), useLocale.toString());
        } else {
          return mf.format(args);
        }
      }
    }
  }

  @Override
  public String getMessage(Locale locale, Object key, Object[] args,
      Function<Locale, String> dfltMsg) {
    load();
    if (key == null) {
      return dfltMsg.apply(locale);
    } else {
      Map<String, MessageFormat> mfMap = holder.get(locale);
      if (mfMap == null) {
        return dfltMsg.apply(locale);
      } else {
        MessageFormat mf = mfMap.get(key);
        if (mf == null) {
          return dfltMsg.apply(locale);
        } else {
          return mf.format(args);
        }
      }
    }
  }

  public synchronized void reload() {
    initialized = false;
    load();
  }

  protected synchronized void clear() {
    holder.forEach((k, v) -> v.clear());
    holder.clear();
    initialized = false;
  }

  protected boolean isInitialized() {
    return initialized;
  }

  protected void load() {
    if (!isInitialized()) {
      synchronized (this) {
        if (!isInitialized()) {
          try {
            clear();
            logger.fine(() -> "Clear property message bundle holder for initializing.");
            Set<String> paths = setOf(split(bundleFilePaths, ","));
            paths.stream().filter(Strings::isNotBlank).forEach(pkg -> {
              PropertyResourceBundle.getBundles(pkg, r -> true).forEach((s, res) -> {
                logger.fine(() -> String.format("Find message resource from %s.", s));
                Map<String, MessageFormat> localeMap = res.dump().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, v -> new MessageFormat(v.getValue())));
                holder.computeIfAbsent(res.getLocale(), k -> new ConcurrentHashMap<>())
                    .putAll(localeMap);
                logger.fine(() -> String.format("Find %s %s message keys from %s.",
                    localeMap.size(), res.getLocale(), s));
              });
            });

          } finally {
            initialized = true;
            logger.fine(() -> String.format("Find %s message keys from %s.",
                holder.values().stream().flatMap(e -> e.values().stream()).count(),
                bundleFilePaths));
          }
        }
      }
    }
  }

  @PostConstruct
  protected synchronized void onPostConstruct() {
    load();
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    clear();
    logger.fine(() -> "Clear property message bundle holder.");
  }
}

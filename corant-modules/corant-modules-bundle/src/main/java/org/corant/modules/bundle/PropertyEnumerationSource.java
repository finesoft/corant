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

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Strings.split;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-bundle
 *
 * @author bingo 上午10:30:07
 *
 */
@ApplicationScoped
@SuppressWarnings("rawtypes")
public class PropertyEnumerationSource implements EnumerationSource {

  public static final String BUNDLE_PATHS_CFG_KEY = "corant.bundle.enum-file.paths";
  public static final String DEFAULT_BUNDLE_PATHS = "META-INF/**Enums_*.properties";

  @Inject
  protected Logger logger;

  protected final Map<Locale, EnumLiteralsObject> holder = new ConcurrentHashMap<>();

  protected volatile boolean initialized = false;

  @Inject
  @ConfigProperty(name = BUNDLE_PATHS_CFG_KEY, defaultValue = DEFAULT_BUNDLE_PATHS)
  protected String bundleFilePaths;

  @Override
  public List<Class<Enum>> getAllEnumClass() {
    return holder.values().stream().flatMap(e -> e.classLiteral.keySet().stream())
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String getEnumClassLiteral(Class<?> enumClass, Locale locale) {
    return holder.get(locale) == null ? null : holder.get(locale).getLiteral(enumClass);
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getEnumItemLiteral(Enum enumVal, Locale locale) {
    load();
    Map<Enum, String> lLiterals =
        getEnumItemLiterals((Class<Enum>) enumVal.getDeclaringClass(), locale);
    if (lLiterals != null) {
      return lLiterals.get(enumVal);
    }
    return enumVal.name();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Enum> Map<T, String> getEnumItemLiterals(Class<T> enumClass, Locale locale) {
    load();
    Map<Enum, String> lLiterals =
        holder.get(locale) == null ? null : holder.get(locale).getLiterals(enumClass);
    return lLiterals == null ? null : new LinkedHashMap(lLiterals); // FIXME unmodifiable pass javac
  }

  public synchronized void reload() {
    initialized = false;
    load();
  }

  protected synchronized void clear() {
    holder.forEach((k, v) -> {
      v.classLiteral.clear();
      v.enumLiterals.clear();
    });
    holder.clear();
    initialized = false;
  }

  protected boolean isInitialized() {
    return initialized;
  }

  @SuppressWarnings("unchecked")
  protected void load() {
    if (!isInitialized()) {
      synchronized (this) {
        if (!isInitialized()) {
          try {
            clear();
            logger.fine(() -> "Clear property enumerations bundle holder for initializing.");
            PropertyResourceBundle.getFoldedLocaleBundles(null, split(bundleFilePaths, ","))
                .forEach((lc, res) -> {
                  EnumLiteralsObject el = holder.computeIfAbsent(lc, k -> new EnumLiteralsObject());
                  res.dump().forEach((k, v) -> {
                    if (!PropertyResourceBundle.PRIORITY_KEY.equals(k)) {
                      int i = k.lastIndexOf('.');
                      String enumClsName = k.substring(0, i);
                      String enumItemKey = null;
                      Class enumCls;
                      try {
                        enumCls = Class.forName(enumClsName);
                        enumItemKey = k.substring(i + 1);
                      } catch (ClassNotFoundException e) {
                        enumCls = tryAsClass(k);
                        if (enumCls != null && Enum.class.isAssignableFrom(enumCls)) {
                          el.putEnumClass(enumCls, v);
                        } else {
                          throw new CorantRuntimeException("Enum class %s on %s error", enumClsName,
                              res.getUri());
                        }
                      }
                      if (enumItemKey != null) {
                        el.putEnum(Enum.valueOf(enumCls, enumItemKey), v);
                      }
                    }
                  });
                });
            // TODO validate
          } finally {
            initialized = true;
            logger.fine(() -> String.format("Found %s enumeration class literals from %s.",
                holder.values().stream().mapToLong(e -> e.classLiteral.size()).sum(),
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
    logger.fine(() -> "Clear property enumerations bundle holder.");
  }

  public static class EnumLiteralsObject {

    Logger logger = Logger.getLogger(EnumLiteralsObject.class.getName());
    private final Map<Class<Enum>, String> classLiteral = new ConcurrentHashMap<>();
    private final Map<Class<Enum>, Map<Enum, String>> enumLiterals = new ConcurrentHashMap<>();

    public String getLiteral(Class clz) {
      return classLiteral.get(clz);
    }

    public Map<Enum, String> getLiterals(Class clz) {
      return enumLiterals.get(clz);
    }

    @SuppressWarnings("unchecked")
    public void putEnum(Enum e, String literal) {
      Class declaringClass = e.getDeclaringClass();
      if (!enumLiterals.containsKey(declaringClass)) {
        enumLiterals.put(declaringClass, new TreeMap<>(Comparator.comparingInt(Enum::ordinal)));
      }
      Map<Enum, String> map = enumLiterals.get(declaringClass);
      if (map.put(e, literal) != null) {
        logger.warning(
            () -> String.format("Enum value [%s] literal description of type [%s] repeats.", e,
                e.getClass().getName()));
      }
    }

    @SuppressWarnings("unchecked")
    public void putEnumClass(Class clz, String literal) {
      if (classLiteral.put(clz, literal) != null) {
        logger.warning(() -> String.format("Enum type [%s] literal description repeated.", clz));
      }
    }
  }
}

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
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author bingo 上午10:30:07
 *
 */
@ApplicationScoped
@SuppressWarnings("rawtypes")
public class PropertyEnumerationSource implements EnumerationSource {

  @Inject
  protected Logger logger;

  protected final Map<Locale, EnumLiteralsObject> holder = new ConcurrentHashMap<>();

  protected volatile boolean initialized = false;

  @Inject
  @ConfigProperty(name = "corant.bundle.enum-file.paths",
      defaultValue = "META-INF/**Enums_*.properties")
  protected String bundleFilePaths;

  @SuppressWarnings("unchecked")
  @Override
  public List<Class<Enum>> getAllEnumClass() {
    return new ArrayList(holder.values().stream().flatMap(e -> e.classLiteral.keySet().stream())
        .collect(Collectors.toSet()));
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
            Set<String> paths = setOf(split(bundleFilePaths, ","));
            paths.stream().filter(Strings::isNotBlank).forEach(path -> {
              PropertyResourceBundle.getBundles(path, r -> true).forEach((s, res) -> {
                logger.fine(() -> String.format("Find enumeration resource from %s.", s));
                Locale locale = res.getLocale();
                EnumLiteralsObject obj =
                    holder.computeIfAbsent(locale, k -> new EnumLiteralsObject());
                res.dump().forEach((k, v) -> {
                  int i = k.lastIndexOf('.');
                  String enumClsName = k.substring(0, i);
                  String enumItemKey = null;
                  Class enumCls = null;
                  try {
                    enumCls = Class.forName(enumClsName);
                    enumItemKey = k.substring(i + 1);
                  } catch (ClassNotFoundException e) {
                    enumCls = tryAsClass(k);
                    if (enumCls != null && Enum.class.isAssignableFrom(enumCls)) {
                      obj.putEnumClass(enumCls, v);
                    } else {
                      throw new CorantRuntimeException("enum class %s error", s);
                    }
                  }
                  if (enumItemKey != null) {
                    obj.putEnum(Enum.valueOf(enumCls, enumItemKey), v);
                  }
                });
              });
            });
            // TODO validate
          } finally {
            initialized = true;
            logger.fine(() -> String.format("Find %s enumeration class literals from %s.",
                holder.values().stream().flatMap(e -> e.classLiteral.keySet().stream()).count(),
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
        enumLiterals.put(declaringClass,
            new TreeMap<Enum, String>((Enum o1, Enum o2) -> o1.ordinal() - o2.ordinal()));
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

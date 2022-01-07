/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.right;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.normal.Defaults;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.URLResource;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Resources;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午3:47:37
 *
 */
public class PropertyResourceBundle extends ResourceBundle implements Sortable {

  public static final String PRIORITY_KEY = "corant.bundle.priority";

  public static final String LOCALE_SPT = "_";

  public static final char LOCALE_SPT_CHAR = '_';

  private static final Logger logger = Logger.getLogger(PropertyResourceBundle.class.getName());

  private final Map<String, Object> lookup;

  private final long lastModifiedTime;

  private final Locale locale;

  private final String baseBundleName;

  private final String uri;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public PropertyResourceBundle(Resource fo) {
    uri = fo instanceof URLResource ? ((URLResource) fo).getURI().toString() : fo.getName();
    baseBundleName = fo.getName();
    locale = PropertyResourceBundle.detectLocaleByName(baseBundleName);
    lastModifiedTime = Instant.now().toEpochMilli();
    Properties properties = new Properties();
    try (InputStream is = fo.openInputStream();
        InputStreamReader isr = new InputStreamReader(is, Defaults.DFLT_CHARSET)) {
      properties.load(isr);
    } catch (IOException e) {
      throw new NoSuchBundleException(e, "Can not load property resource bundle %s.", uri);
    }
    logger.fine(() -> String.format("Load property resource from %s.", fo.getLocation()));
    lookup = new HashMap(properties);
  }

  public static List<PropertyResourceBundle> getBundles(String path, Predicate<Resource> fs) {
    try {
      List<PropertyResourceBundle> list = Resources.from(path).filter(fs).parallel()
          .map(PropertyResourceBundle::new).collect(Collectors.toList());
      list.sort(Sortable::compare);
      return list;
    } catch (IOException e) {
      throw new NoSuchBundleException(e, "Can not load property resource bundles from paths %s.",
          path);
    }
  }

  protected static Locale detectLocaleByName(String name) {
    String useName = defaultStrip(FileUtils.getFileBaseName(name));
    if (isNotBlank(useName) && useName.contains(LOCALE_SPT)) {
      return LocaleUtils.langToLocale(right(useName, 5), LOCALE_SPT_CHAR);
    } else {
      return Locale.getDefault();
    }
  }

  public Map<String, String> dump() {
    Map<String, String> map = new HashMap<>();
    Enumeration<String> msgKeys = getKeys();
    while (msgKeys.hasMoreElements()) {
      String msgKey = msgKeys.nextElement();
      String mfv = getString(msgKey);
      map.put(msgKey, mfv);
    }
    return map;
  }

  @Override
  public String getBaseBundleName() {
    return baseBundleName;
  }

  @Override
  public Enumeration<String> getKeys() {
    ResourceBundle parent = this.parent;
    return new ResourceBundleEnumeration(lookup.keySet(), parent != null ? parent.getKeys() : null);
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  @Override
  public int getPriority() {
    return getMapInteger(lookup, PRIORITY_KEY, Sortable.super.getPriority());
  }

  public String getUri() {
    return uri;
  }

  @Override
  protected Object handleGetObject(String key) {
    if (key == null) {
      throw new NullPointerException();
    }
    return lookup.get(key);
  }

  public static class ResourceBundleEnumeration implements Enumeration<String> {

    Set<String> set;
    Iterator<String> iterator;
    Enumeration<String> enumeration; // may remain null

    String next = null;

    /**
     * Constructs a resource bundle enumeration.
     *
     * @param set a set providing some elements of the enumeration
     * @param enumeration an enumeration providing more elements of the enumeration. enumeration may
     *        be null.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResourceBundleEnumeration(Set<String> set, Enumeration enumeration) {
      this.set = set;
      iterator = set.iterator();
      this.enumeration = enumeration;
    }

    @Override
    public boolean hasMoreElements() {
      if (next == null) {
        if (iterator.hasNext()) {
          next = iterator.next();
        } else if (enumeration != null) {
          while (next == null && enumeration.hasMoreElements()) {
            next = enumeration.nextElement();
            if (set.contains(next)) {
              next = null;
            }
          }
        }
      }
      return next != null;
    }

    @Override
    public String nextElement() {
      if (hasMoreElements()) {
        String result = next;
        next = null;
        return result;
      } else {
        throw new NoSuchElementException();
      }
    }
  }
}

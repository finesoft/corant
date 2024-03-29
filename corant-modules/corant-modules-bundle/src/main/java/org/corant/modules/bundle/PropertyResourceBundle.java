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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.defaultBiPredicate;
import static org.corant.shared.util.Functions.emptyBiPredicate;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.right;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Arrays;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.normal.Defaults;
import org.corant.shared.resource.Resource;
import org.corant.shared.resource.URLResource;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Strings;

/**
 * corant-modules-bundle
 *
 * <p>
 * A simple class can build a {@link ResourceBundle} with priority according to the specified
 * properties {@link Resource}.
 *
 * @see Sortable
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

  public PropertyResourceBundle(Resource fo) {
    this(fo, emptyBiPredicate(true));
  }

  public PropertyResourceBundle(Resource fo, BiPredicate<String, String> fs) {
    uri = fo instanceof URLResource ? ((URLResource) fo).getURI().toString() : fo.getName();
    baseBundleName = fo.getName();
    locale = PropertyResourceBundle.detectLocaleByName(baseBundleName);
    lastModifiedTime = Instant.now().toEpochMilli();
    lookup = new ConcurrentHashMap<>();
    Properties properties = new Properties();
    try (InputStream is = fo.openInputStream();
        InputStreamReader isr = new InputStreamReader(is, Defaults.DFLT_CHARSET)) {
      properties.load(isr);
    } catch (IOException e) {
      throw new NoSuchBundleException(e, "Can not load property resource bundle %s.", uri);
    }
    logger.fine(() -> String.format("Load property resource from %s.", fo.getLocation()));
    final BiPredicate<String, String> ufs = defaultBiPredicate(fs, true);
    properties.forEach((k, v) -> {
      if (k != null) {
        String key = k.toString();
        String val = v == null ? null : v.toString();
        if (ufs.test(key, val)) {
          lookup.put(key, val);
        }
      }
    });
  }

  /**
   * Combine the given property resource bundle list into a locale maps according to the priority
   * order, where the higher priority is the parent.
   *
   * @param list property bundle to be combined
   * @return a combined locale bundle maps
   */
  public static Map<Locale, PropertyResourceBundle> foldedLocaleBundles(
      List<PropertyResourceBundle> list) {
    Map<Locale, PropertyResourceBundle> bundles = new HashMap<>();
    if (isNotEmpty(list)) {
      list.stream().sorted(Sortable::reverseCompare).forEachOrdered(res -> {
        Locale locale = res.getLocale();
        PropertyResourceBundle bundle = bundles.get(locale);
        if (bundle == null) {
          bundles.put(locale, res);
        } else {
          res.setParent(bundle);
          bundle = res;
          bundles.put(locale, bundle);
        }
      });
    }
    return bundles;
  }

  /**
   * Return all property resource bundles scanned according to the given path expression, the given
   * path expression supports class path and file system path with wildcard etc.
   *
   * @param path path expression supports class path and file system path with wildcard etc
   * @see #getBundles(String, BiPredicate)
   */
  public static List<PropertyResourceBundle> getBundles(String path) {
    return getBundles(path, emptyBiPredicate(true));
  }

  /**
   * Return all property resource bundles scanned according to the given path expression, the given
   * path expression supports class path and file system path with wildcard etc.
   *
   * @param path path expression supports class path and file system path with wildcard etc
   * @param fs filter use to filter the properties key and value
   * @return a property resource bundle list
   */
  public static List<PropertyResourceBundle> getBundles(String path,
      BiPredicate<String, String> fs) {
    try {
      return Resources.from(path).parallel().map(r -> new PropertyResourceBundle(r, fs))
          .sorted(Sortable::compare).collect(Collectors.toList());
    } catch (IOException e) {
      throw new NoSuchBundleException(e, "Can not load property resource bundles from paths %s.",
          path);
    }
  }

  /**
   * Returns a locale resource bundle maps combining all property resource bundles scanned according
   * to the given path expression and filtered according to the given property key value filter.
   *
   * @param fs filter use to filter the properties key and value
   * @param paths path expression supports class path and file system path with wildcard etc
   *
   * @see #getBundles(String, BiPredicate)
   * @see #foldedLocaleBundles(List)
   */
  public static Map<Locale, PropertyResourceBundle> getFoldedLocaleBundles(
      BiPredicate<String, String> fs, String... paths) {
    return foldedLocaleBundles(Arrays.stream(paths).filter(Strings::isNotBlank)
        .flatMap(pkg -> getBundles(pkg, fs).stream()).collect(Collectors.toList()));
  }

  /**
   * Returns a locale resource bundle maps combining all property resource bundles scanned according
   * to the given path expression.
   *
   * @param paths path expression supports class path and file system path with wildcard etc
   *
   * @see #getFoldedLocaleBundles(BiPredicate, String...)
   */
  public static Map<Locale, PropertyResourceBundle> getFoldedLocaleBundles(String... paths) {
    return getFoldedLocaleBundles(null, paths);
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

  @Override
  protected void setParent(ResourceBundle parent) {
    super.setParent(parent);
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

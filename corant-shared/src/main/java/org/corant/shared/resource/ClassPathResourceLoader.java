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
package org.corant.shared.resource;

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.immutableSetOf;
import static org.corant.shared.util.Strings.isBlank;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;
import org.corant.shared.util.PathMatcher;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 下午3:38:29
 *
 */
public class ClassPathResourceLoader implements ResourceLoader {

  public static final String SYS_PATH_SEPARATOR = Pattern.quote(Systems.getPathSeparator());
  public static final String SYS_CLASS_PATH = Systems.getProperty("java.class.path");
  public static final String SYS_BOOT_CLASS_PATH = Systems.getProperty("sun.boot.class.path");
  public static final String SYS_EXT_DIRS = Systems.getProperty("java.ext.dirs");
  public static final Set<String> SYS_LIBS =
      immutableSetOf("java", "javax", "javafx", "jdk", "sun", "oracle", "netscape", "org/ietf",
          "org/jcp", "org/omg", "org/w3c", "org/xml", "com/sun", "com/oracle");

  public static final ClassPathResourceLoader DFLT_INST = new ClassPathResourceLoader();

  protected final ClassLoader classLoader;

  protected final boolean ignoreCase;

  public ClassPathResourceLoader() {
    this(null);
  }

  public ClassPathResourceLoader(ClassLoader classLoader) {
    this(defaultObject(classLoader, Classes::defaultClassLoader), true);
  }

  public ClassPathResourceLoader(ClassLoader classLoader, boolean ignoreCase) {
    this.classLoader = classLoader;
    this.ignoreCase = ignoreCase;
  }

  /**
   * Get the resources of a relative path through a class and path
   *
   * @param relative the class to load resources with
   * @param path relative or absolute path within the class path
   * @return the class path resource
   */
  public static URLResource relative(Class<?> relative, String path) {
    return ClassPathResourceScanner.relative(relative, path);
  }

  /**
   * Scan class path resource with the given path or path expression, path separator is '/', allowed
   * for use glob-pattern/regex. If path start with 'glob:' then use Glob pattern else if path start
   * with 'regex:' then use regex pattern; if pattern not found this method will auto decide
   * matcher, if matcher no found then use class loader getResources() and the parameter ignoreCase
   * will be abandoned.
   *
   * <pre>
   * for example:
   * 1.if path is "javax/sql/" then will scan all resources that under the javax.sql class path.
   * 2.if path is "java/sql/Driver.class" then will scan single resource javax.sql.Driver.
   * 3.if path is "META-INF/maven/" then will scan all resources under the META-INF/maven.
   * 4.if path is blank then will scan all class path in the system.
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filter class name
   * end with Driver.class.
   * </pre>
   * <p>
   * NOTE: A leading slash will be removed, as the ClassLoader resource access methods will not
   * accept it. If the supplied ClassLoader is null, the default class loader will be used for
   * loading the resource.
   *
   * @see PathMatcher#decidePathMatcher(String, boolean, boolean)
   *
   * @param location the resource path or expression, supports glob-pattern/regex
   * @return the class path resources that match the given path or expression
   * @throws IOException If I/O errors occur
   */
  @Override
  public Set<ClassPathResource> load(Object location) throws IOException {
    String path = SourceType.CLASS_PATH.resolve(location == null ? null : location.toString());
    Optional<PathMatcher> pathMatcher = PathMatcher.decidePathMatcher(path, false, ignoreCase,
        ClassPathResourceScanner.PATH_SEPARATOR_STRING);
    if (pathMatcher.isPresent()) {
      ClassPathResourceScanner scanner = new ClassPathResourceScanner(pathMatcher.get());
      for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(scanner.getRoot()).entrySet()) {
        scanner.scan(entry.getKey(), entry.getValue());
      }
      return scanner.getResources();
    } else {
      return getClassPathResourceUrls(path).stream().map(u -> {
        try {
          return ClassPathResource.of(u.toURI().getRawSchemeSpecificPart(), classLoader, u);
        } catch (URISyntaxException e) {
          throw new CorantRuntimeException(e);
        }
      }).collect(Collectors.toSet());
    }
  }

  protected Map<URI, ClassLoader> getClassPathEntries(String path) {
    LinkedHashMap<URI, ClassLoader> entries = new LinkedHashMap<>();
    try {
      for (URL url : getClassPathResourceUrls(path)) {
        entries.putIfAbsent(url.toURI(), classLoader);
      }
      ClassLoader currClsLoader = classLoader;
      do {
        if (currClsLoader instanceof URLClassLoader currUrlClsLoader) {
          for (URL entry : currUrlClsLoader.getURLs()) {
            entries.putIfAbsent(entry.toURI(), currClsLoader);
          }
        }
        if (currClsLoader.equals(ClassLoader.getSystemClassLoader())) {
          Set<String> sysClsPaths = new LinkedHashSet<>();
          if (SYS_CLASS_PATH != null) {
            Collections.addAll(sysClsPaths, SYS_CLASS_PATH.split(SYS_PATH_SEPARATOR));
          }
          if (loadAll(path)) {
            if (SYS_BOOT_CLASS_PATH != null) {
              sysClsPaths.addAll(listOf(SYS_BOOT_CLASS_PATH.split(SYS_PATH_SEPARATOR)));
            }
            if (SYS_EXT_DIRS != null) {
              sysClsPaths.addAll(listOf(SYS_EXT_DIRS.split(SYS_PATH_SEPARATOR)));
            }
          }
          for (String classPath : sysClsPaths) {
            try {
              entries.putIfAbsent(new File(classPath).toURI(), currClsLoader);
            } catch (SecurityException e) {
              entries.putIfAbsent(new URL(ClassPathResourceScanner.FILE_SCHEMA, null,
                  new File(classPath).getAbsolutePath()).toURI(), currClsLoader);
            }
          }
        }
      } while ((currClsLoader = currClsLoader.getParent()) != null);
    } catch (IOException | URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    return entries;
  }

  protected List<URL> getClassPathResourceUrls(String classPath) {
    List<URL> entries = new ArrayList<>();
    try {
      Enumeration<URL> urls = classLoader.getResources(classPath);
      while (urls.hasMoreElements()) {
        entries.add(urls.nextElement());
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return entries;
  }

  protected boolean loadAll(String path) {
    return isBlank(path) || SYS_LIBS.stream().anyMatch(path::startsWith);
  }
}

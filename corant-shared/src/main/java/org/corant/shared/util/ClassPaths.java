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
package org.corant.shared.util;

import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.CollectionUtils.asImmutableSet;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.replace;
import static org.corant.shared.util.StringUtils.split;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * corant-shared
 *
 * @author bingo 上午11:21:01
 *
 */
public class ClassPaths {

  public static final String JAR_URL_SEPARATOR = "!/";
  public static final char PATH_SEPARATOR = '/';
  public static final String PATH_SEPARATOR_STRING = Character.toString(PATH_SEPARATOR);
  public static final String CLASSES_FOLDER = "classes" + File.separator;
  private static final Logger LOGGER = Logger.getLogger(ClassPaths.class.getName());

  /**
   * Use default classloader to scan all class path resources.
   *
   * @see #from(ClassLoader, String)
   * @see ClassUtils#defaultClassLoader()
   * @return
   * @throws IOException from
   */
  public static ClassPath from() throws IOException {
    return from(defaultClassLoader(), StringUtils.EMPTY);
  }

  /**
   * Use specified to scan all class path resources.
   *
   * @see #from(ClassLoader, String)
   *
   * @param classLoader
   * @return
   * @throws IOException from
   */
  public static ClassPath from(ClassLoader classLoader) throws IOException {
    return from(classLoader, StringUtils.EMPTY);
  }

  /**
   * Scan class path resource with path, path separator is '/'
   *
   * <pre>
   * for example:
   * 1.if path is "javax/sql" then will scan all resources that under the javax.sql class path.
   * 2.if path is "java/sql/Driver.class" then will scan single resource javax.sql.Driver.
   * 3.if path is "META-INF/maven" then will scan all resources under the META-INF/maven.
   * 4.if path is blank ({@code StringUtils.isBlank}) then will scan all class path in the system.
   * </pre>
   *
   * @param classLoader
   * @param path
   * @return
   * @throws IOException from
   */
  public static ClassPath from(ClassLoader classLoader, String path) throws IOException {
    String root = defaultString(path);
    Scanner scanner = new Scanner(root);
    for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(
        defaultObject(classLoader, defaultClassLoader()), root).entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return new ClassPath(scanner.getResources());
  }

  public static ClassPath from(String path) throws IOException {
    return from(defaultClassLoader(), path);
  }

  public static ClassPath fromAnyway(ClassLoader classLoader) {
    return fromAnyway(classLoader, StringUtils.EMPTY);
  }

  public static ClassPath fromAnyway(ClassLoader classLoader, String path) {
    try {
      return from(classLoader, path);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      return ClassPath.empty();
    }
  }

  public static ClassPath fromAnyway(String path) {
    return fromAnyway(defaultClassLoader(), path);
  }

  static Map<URI, ClassLoader> getClassPathEntries(ClassLoader classLoader, String path) {
    LinkedHashMap<URI, ClassLoader> entries = new LinkedHashMap<>();
    try {
      Enumeration<URL> urls = shouldNotNull(classLoader).getResources(path);
      while (urls.hasMoreElements()) {
        entries.putIfAbsent(urls.nextElement().toURI(), classLoader);
      }
      if (loadAll(path)) {
        ClassLoader currClsLoader = classLoader;
        do {
          if (currClsLoader instanceof URLClassLoader) {
            for (URL entry : URLClassLoader.class.cast(currClsLoader).getURLs()) {
              entries.putIfAbsent(entry.toURI(), currClsLoader);
            }
          }
          if (currClsLoader.equals(ClassLoader.getSystemClassLoader())) {
            for (String classPath : split(System.getProperty("java.class.path"),
                System.getProperty("path.separator"))) {
              try {
                entries.putIfAbsent(new File(classPath).toURI(), currClsLoader);
              } catch (SecurityException e) {
                entries.putIfAbsent(
                    new URL("file", null, new File(classPath).getAbsolutePath()).toURI(),
                    currClsLoader);
              }
            }
          }
        } while ((currClsLoader = currClsLoader.getParent()) != null);
      }
    } catch (IOException | URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    return entries;
  }

  static boolean loadAll(String path) {
    final String sysEnvLib = "java;javax;javafx;jdk;sun;oracle;netscape;"
        + "org/ietf;org/jcp;org/omg;org/w3c;org/xml;com/sun;com/oracle;";
    return isBlank(path) || asStream(split(sysEnvLib, ";")).anyMatch(sp -> path.startsWith(sp));
  }

  public static final class ClassInfo extends ResourceInfo {

    private final String className;

    public ClassInfo(String resourceName, ClassLoader loader) {
      super(resourceName, loader);
      int classNameEnd = resourceName.length() - ClassUtils.CLASS_FILE_NAME_EXTENSION.length();
      className = resourceName.substring(0, classNameEnd).replace(PATH_SEPARATOR,
          ClassUtils.PACKAGE_SEPARATOR_CHAR);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!super.equals(obj)) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ClassInfo other = (ClassInfo) obj;
      if (className == null) {
        if (other.className != null) {
          return false;
        }
      } else if (!className.equals(other.className)) {
        return false;
      }
      return true;
    }

    public String getName() {
      return className;
    }

    public String getPackageName() {
      return ClassUtils.getPackageName(className);
    }

    public String getSimpleName() {
      return ClassUtils.getShortClassName(className);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (className == null ? 0 : className.hashCode());
      return result;
    }

    public Class<?> load() {
      try {
        return loader.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String toString() {
      return className;
    }

  }

  public static final class ClassPath {

    static final ClassPath EMPTY_INSTANCE = new ClassPath(Collections.emptySet());

    final Set<ResourceInfo> resources;

    private ClassPath(Set<ResourceInfo> resources) {
      this.resources = resources;
    }

    public static ClassPath empty() {
      return EMPTY_INSTANCE;
    }

    public Stream<ClassInfo> getClasses() {
      return getResources().filter(ClassInfo.class::isInstance).map(ClassInfo.class::cast);
    }

    public ResourceInfo getResource(Predicate<ResourceInfo> filter) {
      Predicate<ResourceInfo> usedFilter = filter == null ? (r) -> true : filter;
      return asStream(resources).filter(usedFilter).findFirst().orElse(null);
    }

    public Stream<ResourceInfo> getResources() {
      if (resources == null) {
        return Stream.empty();
      } else {
        return asStream(resources);
      }
    }

    public Stream<ClassInfo> getTopLevelClasses() {
      return getClasses()
          .filter(ci -> ci.getName().indexOf(ClassUtils.INNER_CLASS_SEPARATOR) == -1);
    }
  }

  public static class ResourceInfo {

    final ClassLoader loader;

    private final String resourceName;

    public ResourceInfo(String resourceName, ClassLoader loader) {
      this.resourceName = shouldNotNull(resourceName);
      this.loader = shouldNotNull(loader);
    }

    static ResourceInfo of(String resourceName, ClassLoader loader) {
      if (resourceName.endsWith(ClassUtils.CLASS_FILE_NAME_EXTENSION)) {
        return new ClassInfo(resourceName, loader);
      } else {
        return new ResourceInfo(resourceName, loader);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ResourceInfo) {
        ResourceInfo that = (ResourceInfo) obj;
        return resourceName.equals(that.resourceName) && loader == that.loader;
      }
      return false;
    }

    /**
     * Returns the fully qualified name of the resource. Such as "com/mycomp/foo/bar.txt".
     */
    public final String getResourceName() {
      return resourceName;
    }

    public final URL getUrl() {
      return loader.getResource(resourceName);
    }

    @Override
    public int hashCode() {
      return resourceName.hashCode();
    }

    public final InputStream openStream() throws IOException {
      URLConnection conn = getUrl().openConnection();
      if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("window")) {
        conn.setUseCaches(false);
      }
      return conn.getInputStream();
    }

    // Do not change this arbitrarily. We rely on it for sorting ResourceInfo.
    @Override
    public String toString() {
      return resourceName;
    }
  }

  static final class Scanner {

    private final Set<ResourceInfo> resources = new LinkedHashSet<>();
    private final Set<URI> scannedUris = new HashSet<>();
    private final String root;

    Scanner() {
      this("");
    }

    /**
     * @param root
     */
    Scanner(String root) {
      super();
      this.root = root;
    }

    URI getClassPathEntry(File jarFile, String path) throws URISyntaxException {
      URI uri = new URI(path);
      if (uri.isAbsolute()) {
        return uri;
      } else {
        return new File(jarFile.getParentFile(), path.replace('/', File.separatorChar)).toURI();
      }
    }

    Set<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
      String attrName = Attributes.Name.CLASS_PATH.toString();
      Attributes attrs = null;
      if (manifest == null || (attrs = manifest.getMainAttributes()) == null
          || !attrs.containsKey(attrName)) {
        return asImmutableSet();
      }
      Set<URI> uriSet = new LinkedHashSet<>();
      for (String path : split(attrs.getValue(attrName), " ")) {
        try {
          uriSet.add(getClassPathEntry(jarFile, path));
        } catch (URISyntaxException e) {
          LOGGER.warning(() -> "Invalid Class-Path entry: " + path);
          continue;
        }
      }
      return uriSet;
    }

    Set<ResourceInfo> getResources() {
      return resources;
    }

    void scan(URI uri, ClassLoader classloader) throws IOException {
      if (uri.getScheme().equals("file") && scannedUris.add(uri)) {
        scanFrom(new File(uri).getCanonicalFile(), classloader);
      } else if (uri.getScheme().equals("jar")) {
        URI exUri = tryExtractFileUri(uri);
        if (exUri != null && scannedUris.add(exUri)) {
          scanFrom(new File(exUri).getCanonicalFile(), classloader);
        }
      }
    }

    void scanFrom(File file, ClassLoader classloader) throws IOException {
      if (!file.exists()) {
        return;
      }
      if (file.isDirectory()) {
        scanDirectory(file, classloader);
      } else if (file.getCanonicalPath().toLowerCase(Locale.getDefault()).endsWith(".jar")) {
        scanJar(file, classloader);
      } else {
        scanSingleFile(file, classloader);
      }
    }

    private void scanDirectory(File directory, ClassLoader classloader) throws IOException {
      String packagePrefix =
          isNotBlank(root) && !root.endsWith(PATH_SEPARATOR_STRING) ? root + PATH_SEPARATOR_STRING
              : root;
      scanDirectory(directory, classloader, packagePrefix, new LinkedHashSet<>());
    }

    private void scanDirectory(File directory, ClassLoader classloader, String packagePrefix,
        Set<File> ancestors) throws IOException {
      File canonical = directory.getCanonicalFile();
      if (ancestors.contains(canonical)) {
        return;
      }
      File[] files = directory.listFiles();
      if (files == null) {
        LOGGER.warning(() -> "Cannot read directory " + directory);
        return;
      }
      Set<File> fileSet = new LinkedHashSet<>(ancestors);
      fileSet.add(canonical);
      String cmprPackagePrefix = replace(packagePrefix, PATH_SEPARATOR_STRING, File.separator);
      for (File f : files) {
        String name = f.getName();
        if (!isBlank(packagePrefix) && !f.getPath().endsWith(cmprPackagePrefix + name)) {
          continue;
        }
        if (f.isDirectory()) {
          scanDirectory(f, classloader, packagePrefix + name + PATH_SEPARATOR, fileSet);
        } else {
          String resourceName = packagePrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
            resources.add(ResourceInfo.of(resourceName, classloader));
          }
        }
      }
    }

    private void scanJar(File file, ClassLoader classloader) throws IOException {
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException notJarFile) {
        LOGGER.warning(() -> String.format("The file %s is not jar file!", file.getName()));
        return;
      }
      try {
        for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(uri, classloader);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)
              || isNotBlank(root) && !entry.getName().startsWith(root)) {
            continue;
          }
          resources.add(ResourceInfo.of(entry.getName(), classloader));
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {
        }
      }
    }

    private void scanSingleFile(File file, ClassLoader classloader) throws IOException {
      if (!file.getCanonicalPath().equals(JarFile.MANIFEST_NAME)) {
        String canonicalPath = file.getCanonicalPath();
        int classesIdx = -1;
        if ((classesIdx = canonicalPath.indexOf(CLASSES_FOLDER)) != -1) {
          canonicalPath = canonicalPath.substring(classesIdx + CLASSES_FOLDER.length());
        }
        resources.add(ResourceInfo.of(replace(canonicalPath, File.separator, PATH_SEPARATOR_STRING),
            classloader));
      }
    }

    private URI tryExtractFileUri(URI jarUri) {
      try {
        String specPart = jarUri.getSchemeSpecificPart();
        while (specPart != null) {
          URI fileUri = new URL(specPart).toURI();
          if ("file".equals(fileUri.getScheme())) {
            String fileUrlStr = fileUri.toURL().toExternalForm();
            int sp = fileUrlStr.indexOf(JAR_URL_SEPARATOR);
            if (sp != -1) {
              fileUrlStr = fileUrlStr.substring(0, sp);
            }
            return new URI(fileUrlStr);
          }
          specPart = fileUri.getSchemeSpecificPart();
        }
      } catch (MalformedURLException | URISyntaxException ignore) {
        LOGGER.warning(() -> String.format("Can not extract file uri from %s.", jarUri.toString()));
      }
      return null;
    }
  }
}

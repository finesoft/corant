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
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.PathUtils.GlobMatcher;
import org.corant.shared.util.PathUtils.GlobPatterns;

/**
 * corant-shared
 *
 * @author bingo 上午11:21:01
 *
 */
public class ClassPaths {

  public static final char PATH_SEPARATOR = '/';
  public static final String JAR_URL_SEPARATOR = "!/";
  public static final String PATH_SEPARATOR_STRING = Character.toString(PATH_SEPARATOR);
  public static final String CLASSES = "classes";
  public static final String CLASSES_FOLDER = CLASSES + File.separator;
  public static final String JAR_EXT = ".jar";
  public static final String WAR_EXT = ".war";
  public static final String META_INF = "META-INF";
  public static final String LIB = "lib";
  public static final String WEB_INF = "WEB-INF";
  public static final String FILE_SCHEMA = "file";
  public static final String JAR_SCHEMA = "jar";
  public static final Map<Path, URLClassLoader> CACHED_CLASS_LOADERS = new ConcurrentHashMap<>();
  public static final Set<String> SYS_LIBS =
      asImmutableSet("java", "javax", "javafx", "jdk", "sun", "oracle", "netscape", "org/ietf",
          "org/jcp", "org/omg", "org/w3c", "org/xml", "com/sun", "com/oracle");

  private static final Logger logger = Logger.getLogger(ClassPaths.class.getName());

  public static ClassPath anyway(ClassLoader classLoader) {
    return anyway(classLoader, StringUtils.EMPTY);
  }

  public static ClassPath anyway(ClassLoader classLoader, String path) {
    try {
      return from(classLoader, path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return ClassPath.empty();
    }
  }

  public static ClassPath anyway(String path) {
    return anyway(defaultClassLoader(), path);
  }

  public static Scanner buildScanner(String pathExpress, boolean ignoreCase) {
    if (GlobMatcher.hasGlobChar(pathExpress)) {
      PathFilter pf = new PathFilter(ignoreCase, pathExpress);
      return new Scanner(pf.getRoot(), pf);
    } else {
      return new Scanner(pathExpress);
    }
  }

  /**
   * Careful use may result in leakage
   *
   * @param path
   * @param parentClassLoader
   * @return
   * @throws IOException buildWarClassLoader
   */
  public static synchronized URLClassLoader buildWarClassLoader(Path path,
      final ClassLoader parentClassLoader) throws IOException {
    shouldBeTrue(path != null, "Build war class loader error path is null");
    if (CACHED_CLASS_LOADERS.containsKey(path)) {
      return CACHED_CLASS_LOADERS.get(path);
    }
    Path tmpDir = Files.createTempDirectory(asString(path.getFileName()));
    // We only extract the WEB-INF folder, it is class path resource.
    FileUtils.extractJarFile(path, tmpDir, (e) -> e.getName().contains(WEB_INF));
    final List<URL> urls = new ArrayList<>();
    if (shouldNotNull(tmpDir.toFile()).exists()) {
      Path webInf = tmpDir.resolve(WEB_INF);
      if (webInf.toFile().canRead()) {
        Path classes = webInf.resolve(CLASSES);
        Path lib = webInf.resolve(LIB);
        Path metaInf = webInf.resolve(META_INF);
        if (metaInf.toFile().canRead()) {
          urls.add(metaInf.toUri().toURL());
        }
        if (classes.toFile().canRead()) {
          urls.add(classes.toUri().toURL());
        }
        if (lib.toFile().canRead()) {
          Files.walkFileTree(lib, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              if (file != null && asString(file.getFileName()).endsWith(JAR_EXT)) {
                urls.add(file.toUri().toURL());
              }
              return FileVisitResult.CONTINUE;
            }
          });
        }
      }
      tmpDir.toFile().deleteOnExit();
    }
    return CACHED_CLASS_LOADERS.put(path,
        AccessController.doPrivileged((PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(
            urls.toArray(new URL[urls.size()]), parentClassLoader)));
  }

  /**
   * Use default classLoader to scan all class path resources.
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
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filte class name
   * end with Driver.class.
   * </pre>
   *
   * @param classLoader
   * @param path
   * @return
   * @throws IOException from
   */
  public static ClassPath from(ClassLoader classLoader, String path) throws IOException {
    Scanner scanner = buildScanner(defaultString(path), false);
    for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(
        defaultObject(classLoader, defaultClassLoader()), scanner.getRoot()).entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return new ClassPath(scanner.getResources());
  }

  public static ClassPath from(String path) throws IOException {
    return from(defaultClassLoader(), path);
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
                    new URL(FILE_SCHEMA, null, new File(classPath).getAbsolutePath()).toURI(),
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
    return isBlank(path) || SYS_LIBS.stream().anyMatch(sp -> path.startsWith(sp));
  }

  /**
   * corant-shared
   *
   * Describe class resource, but doesn't load it right away.
   *
   * @author bingo 下午8:35:36
   *
   */
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

  /**
   * corant-shared
   *
   * Describe class path resources
   *
   * @author bingo 下午8:33:44
   *
   */
  public static final class ClassPath {

    static final ClassPath EMPTY_INSTANCE = new ClassPath(Collections.emptySet());
    final Set<ResourceInfo> resources;

    private ClassPath(Set<ResourceInfo> resources) {
      this.resources = new LinkedHashSet<>(resources);
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

  /**
   * corant-shared
   *
   * Use wildcards for filtering, algorithm from apache.org.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static final class PathFilter extends GlobMatcher {

    protected PathFilter(boolean ignoreCase, String pathExpress) {
      super(ignoreCase, pathExpress);
    }

    public String getRoot() {
      String pathExpress = getGlobExpress();
      int len = pathExpress.length();
      int idx = -1;
      for (int i = 0; i < len; i++) {
        if (GlobPatterns.isGlobChar(pathExpress.charAt(i))) {
          idx = i;
          break;
        }
      }
      if (idx == -1) {
        return pathExpress;
      } else if (idx == 0) {
        return StringUtils.EMPTY;
      } else {
        String path = pathExpress.substring(0, idx);
        if (path.indexOf(PATH_SEPARATOR_STRING) != -1) {
          return path.substring(0, path.lastIndexOf(PATH_SEPARATOR));
        } else {
          return path;
        }
      }
    }
  }

  /**
   * corant-shared
   *
   * Describe class path resource include class resource.
   *
   * @author bingo 下午8:37:56
   *
   */
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

    @Override
    public String toString() {
      return resourceName;
    }
  }

  /**
   * corant-shared
   *
   * Scan resource from path, include nested jars.
   *
   * @author bingo 下午8:39:26
   *
   */
  public static final class Scanner {

    private final Set<ResourceInfo> resources = new LinkedHashSet<>();
    private final Set<URI> scannedUris = new HashSet<>();
    private final String root;
    private Predicate<String> filter = s -> true;

    public Scanner(String root) {
      super();
      this.root = root;
    }

    public Scanner(String root, Predicate<String> filter) {
      this(root);
      if (filter != null) {
        this.filter = filter;
      }
    }

    public Set<ResourceInfo> getResources() {
      return resources;
    }

    public String getRoot() {
      return root;
    }

    protected URI getClassPathEntry(File jarFile, String path) throws URISyntaxException {
      URI uri = new URI(path);
      if (uri.isAbsolute()) {
        return uri;
      } else {
        return new File(jarFile.getParentFile(), path.replace(PATH_SEPARATOR, File.separatorChar))
            .toURI();
      }
    }

    protected Set<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
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
          logger.warning(() -> "Invalid Class-Path entry: " + path);
          continue;
        }
      }
      return uriSet;
    }

    protected void scan(URI uri, ClassLoader classloader) throws IOException {
      if (uri.getScheme().equals(FILE_SCHEMA) && scannedUris.add(uri)) {
        scanFrom(new File(uri).getCanonicalFile(), classloader);
      } else if (uri.getScheme().equals(JAR_SCHEMA)) {
        URI exUri = tryExtractFileUri(uri);
        if (exUri != null && scannedUris.add(exUri)) {
          scanFrom(new File(exUri).getCanonicalFile(), classloader);
        }
      }
    }

    protected void scanDirectory(File directory, ClassLoader classloader) throws IOException {
      scanDirectory(directory, classloader,
          isNotBlank(root) && !root.endsWith(PATH_SEPARATOR_STRING) ? root + PATH_SEPARATOR_STRING
              : root,
          new LinkedHashSet<>());
    }

    protected void scanDirectory(File directory, ClassLoader classloader, String packagePrefix,
        Set<File> ancestors) throws IOException {
      File canonical = directory.getCanonicalFile();
      if (ancestors.contains(canonical)) {
        return;
      }
      File[] files = directory.listFiles();
      if (files == null) {
        logger.warning(() -> "Cannot read directory " + directory);
        return;
      }
      Set<File> fileSet = new LinkedHashSet<>(ancestors);
      fileSet.add(canonical);
      for (File f : files) {
        String name = f.getName();
        if (f.isDirectory()) {
          scanDirectory(f, classloader, packagePrefix + name + PATH_SEPARATOR, fileSet);
        } else {
          String resourceName = packagePrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME) && filter.test(resourceName)) {
            resources.add(ResourceInfo.of(resourceName, classloader));
          }
        }
      }
    }

    protected void scanFrom(File file, ClassLoader classloader) throws IOException {
      if (!file.exists()) {
        return;
      }
      if (file.isDirectory()) {
        scanDirectory(file, classloader);
      } else if (file.getCanonicalPath().toLowerCase(Locale.getDefault()).endsWith(JAR_EXT)) {
        scanJar(file, classloader);
      } else if (file.getCanonicalPath().toLowerCase(Locale.getDefault()).endsWith(WAR_EXT)) {
        // To adapt spring boot, a simple and crude and experimental implementation :)
        scanWar(file, classloader);
      } else {
        scanSingleFile(file, classloader);
      }
    }

    protected void scanJar(File file, ClassLoader classloader) throws IOException {
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException notJarFile) {
        logger.warning(() -> String.format("The file %s is not jar file!", file.getName()));
        return;
      }
      try {
        for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(uri, classloader);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();
          if (entry.isDirectory() || name.equals(JarFile.MANIFEST_NAME)
              || isNotBlank(root) && !name.startsWith(root) || !filter.test(name)) {
            continue;
          }
          resources.add(ResourceInfo.of(name, classloader));
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {
        }
      }
    }

    protected void scanSingleFile(File file, ClassLoader classloader) throws IOException {
      if (!file.getCanonicalPath().equals(JarFile.MANIFEST_NAME)) {
        String filePath = file.getCanonicalPath();
        int classesIdx = -1;
        if ((classesIdx = filePath.indexOf(CLASSES_FOLDER)) != -1) {
          filePath = filePath.substring(classesIdx + CLASSES_FOLDER.length());
        }
        String resourceName = replace(filePath, File.separator, PATH_SEPARATOR_STRING);
        if (filter.test(resourceName)) {
          resources.add(ResourceInfo.of(resourceName, classloader));
        }
      }
    }

    protected void scanWar(File file, ClassLoader parent) throws IOException {
      // We need to build an temporary war class loader;
      Scanner warScanner = new Scanner(root, filter);
      final URLClassLoader warClassLoader = buildWarClassLoader(file.toPath(), parent);
      for (URL url : warClassLoader.getURLs()) {
        try {
          warScanner.scan(url.toURI(), warClassLoader);
        } catch (URISyntaxException e) {
          throw new CorantRuntimeException(e);
        }
      }
      warScanner.resources.forEach(resources::add);
    }

    protected URI tryExtractFileUri(URI jarUri) {
      try {
        String specPart = jarUri.getSchemeSpecificPart();
        while (specPart != null) {
          URI fileUri = new URL(specPart).toURI();
          if (FILE_SCHEMA.equals(fileUri.getScheme())) {
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
        logger.warning(() -> String.format("Can not extract file uri from %s.", jarUri.toString()));
      }
      return null;
    }
  }
}

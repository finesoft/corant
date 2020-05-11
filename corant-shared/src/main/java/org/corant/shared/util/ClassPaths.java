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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.CollectionUtils.immutableSetOf;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.replace;
import static org.corant.shared.util.StringUtils.split;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.PathUtils.PathMatcher;
import org.corant.shared.util.Resources.ClassPathResource;

/**
 * corant-shared
 *
 * Utility class for scanning/extracting resources from the classpath.
 *
 * @author bingo 上午11:21:01
 *
 */
public class ClassPaths {

  public static final char PATH_SEPARATOR = '/';
  public static final String JAR_URL_SEPARATOR = "!/";
  public static final String PATH_SEPARATOR_STRING = Character.toString(PATH_SEPARATOR);
  public static final String CLASSES = "classes";
  public static final String CLASSES_FOLDER = CLASSES + PATH_SEPARATOR_STRING;
  public static final String JAR_EXT = ".jar";
  public static final String WAR_EXT = ".war";
  public static final String META_INF = "META-INF";
  public static final String LIB = "lib";
  public static final String WEB_INF = "WEB-INF";
  public static final String FILE_SCHEMA = "file";
  public static final String JAR_SCHEMA = "jar";

  protected static final Set<String> sysLibs =
      immutableSetOf("java", "javax", "javafx", "jdk", "sun", "oracle", "netscape", "org/ietf",
          "org/jcp", "org/omg", "org/w3c", "org/xml", "com/sun", "com/oracle");
  protected static final Map<Path, URLClassLoader> cachedClassLoaders = new ConcurrentHashMap<>();// static?

  private static final Logger logger = Logger.getLogger(ClassPaths.class.getName());

  private ClassPaths() {
    super();
  }

  /**
   * Build an war class loader, Careful use may result in leakage. We only extract WEB-INF folder to
   * default temporary-file directory.
   *
   * @param path
   * @param parentClassLoader
   * @see Files#createTempDirectory(String, java.nio.file.attribute.FileAttribute...)
   * @return The war class loader
   * @throws IOException buildWarClassLoader
   */
  public static synchronized URLClassLoader buildWarClassLoader(Path path,
      final ClassLoader parentClassLoader) throws IOException {
    shouldBeTrue(path != null, "Build war class loader error path is null");
    if (cachedClassLoaders.containsKey(path)) {
      return cachedClassLoaders.get(path);
    }
    Path tmpDir = Files.createTempDirectory(asString(path.getFileName()));
    // We only extract the WEB-INF folder, it is class path resource.
    FileUtils.extractJarFile(path, tmpDir, e -> e.getName().contains(WEB_INF));
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
    return cachedClassLoaders.put(path,
        AccessController.doPrivileged((PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(
            urls.toArray(new URL[urls.size()]), parentClassLoader)));
  }

  /**
   * Scan class path resource with path, path separator is '/', allowed for use glob-pattern/regex,
   * case insensitive.
   *
   * @see #from(ClassLoader, String, boolean)
   * @param classLoader
   * @param path
   * @return
   * @throws IOException from
   */
  public static Set<ClassPathResource> from(ClassLoader classLoader, String path)
      throws IOException {
    return from(classLoader, path, true);
  }

  /**
   * Scan class path resource with path, path separator is '/', allowed for use glob-pattern/regex.
   * If path start with 'glob:' then use Glob pattern else if path start with 'regex:' then use
   * regex pattern; if pattern not found this method will auto decide matcher, if matcher no found
   * then use class loader getResources() and the parameter ignoreCase will be abandoned.
   *
   * <pre>
   * for example:
   * 1.if path is "javax/sql/" then will scan all resources that under the javax.sql class path.
   * 2.if path is "java/sql/Driver.class" then will scan single resource javax.sql.Driver.
   * 3.if path is "META-INF/maven/" then will scan all resources under the META-INF/maven.
   * 4.if path is blank ({@code StringUtils.isBlank}) then will scan all class path in the system.
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filter class name
   * end with Driver.class.
   * </pre>
   *
   * @see PathUtils#decidePathMatcher(String, false, boolean)
   *
   * @param classLoader
   * @param path
   * @param ignoreCase only use in pattern matcher
   * @return
   * @throws IOException from
   */
  public static Set<ClassPathResource> from(ClassLoader classLoader, String path,
      boolean ignoreCase) throws IOException {
    final ClassLoader useClassLoader = defaultObject(classLoader, defaultClassLoader());
    final Optional<PathMatcher> pathMatcher = PathUtils.decidePathMatcher(path, false, ignoreCase);
    if (pathMatcher.isPresent()) {
      Scanner scanner = new Scanner(new ClassPathMatcher(pathMatcher.get()));
      for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(useClassLoader,
          scanner.getRoot()).entrySet()) {
        scanner.scan(entry.getKey(), entry.getValue());
      }
      return scanner.getResources();
    } else {
      return getClassPathResourceUrls(useClassLoader, path).stream().map(u -> {
        try {
          return ClassPathResource.of(u.toURI().getRawSchemeSpecificPart(), classLoader, u);
        } catch (URISyntaxException e) {
          throw new CorantRuntimeException(e);
        }
      }).collect(Collectors.toSet());
    }
  }

  /**
   *
   * @param relative
   * @param path
   * @param ignoreCase
   * @return fromRelative
   */
  public static Set<ClassPathResource> fromRelative(Class<?> relative, String path,
      boolean ignoreCase) {
    throw new NotSupportedException();// TODO
  }

  static Map<URI, ClassLoader> getClassPathEntries(ClassLoader classLoader, String path) {
    LinkedHashMap<URI, ClassLoader> entries = new LinkedHashMap<>();
    try {
      for (URL url : getClassPathResourceUrls(classLoader, path)) {
        entries.putIfAbsent(url.toURI(), classLoader);
      }
      if (loadAll(path)) {
        ClassLoader currClsLoader = classLoader;
        do {
          if (currClsLoader instanceof URLClassLoader) {
            @SuppressWarnings("resource")
            URLClassLoader currUrlClsLoader = (URLClassLoader) currClsLoader;
            for (URL entry : currUrlClsLoader.getURLs()) {
              entries.putIfAbsent(entry.toURI(), currClsLoader);
            }
          }
          if (currClsLoader.equals(ClassLoader.getSystemClassLoader())) {
            Set<String> sysClassPaths = new HashSet<>();
            sysClassPaths.addAll(Arrays.asList(split(System.getProperty("sun.boot.class.path"),
                System.getProperty("path.separator"))));
            sysClassPaths.addAll(Arrays.asList(
                split(System.getProperty("java.ext.dirs"), System.getProperty("path.separator"))));
            sysClassPaths.addAll(Arrays.asList(split(System.getProperty("java.class.path"),
                System.getProperty("path.separator"))));
            for (String classPath : sysClassPaths) {
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

  static List<URL> getClassPathResourceUrls(ClassLoader classLoader, String classPath) {
    List<URL> entries = new ArrayList<>();
    try {
      Enumeration<URL> urls = shouldNotNull(classLoader).getResources(classPath);
      while (urls.hasMoreElements()) {
        entries.add(urls.nextElement());
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return entries;
  }

  private static boolean loadAll(String path) {
    return isBlank(path) || sysLibs.stream().anyMatch(path::startsWith);
  }

  /**
   * corant-shared
   *
   * Use glob/regex express for filtering.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static final class ClassPathMatcher implements Predicate<String> {

    final PathMatcher matcher;
    final String root;

    protected ClassPathMatcher(PathMatcher matcher) {
      this.matcher = matcher;
      root = matcher.getPlainParent(PATH_SEPARATOR_STRING);
    }

    public String getRoot() {
      return root;
    }

    @Override
    public boolean test(String t) {
      return matcher.test(t);
    }
  }

  /**
   * corant-shared
   *
   * Scan resources from class path, include nested jars.
   *
   * @author bingo 下午8:39:26
   *
   */
  public static final class Scanner {

    private final Set<ClassPathResource> resources = new LinkedHashSet<>();
    private final Set<URI> scannedUris = new HashSet<>();
    private final String root;
    private Predicate<String> filter = s -> true;

    public Scanner(ClassPathMatcher matcher) {
      this(matcher.getRoot(), matcher);
    }

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

    public Set<ClassPathResource> getResources() {
      return resources;
    }

    public String getRoot() {
      return root;
    }

    /**
     * Unfinish yet // FIXME
     *
     * @param jarFile
     * @param path
     * @return
     * @throws URISyntaxException getClassPathEntry
     */
    protected URI getClassPathEntry(File jarFile, String path) throws URISyntaxException {
      URI uri = new URI(path);
      if (uri.isAbsolute()) {
        return uri;
      } else {
        return new File(jarFile.getParentFile(), path.replace(PATH_SEPARATOR, File.separatorChar))
            .toURI();
      }
    }

    /**
     * Unfinish yet // FIXME
     *
     * @param jarFile
     * @param manifest
     * @return getClassPathFromManifest
     */
    protected Set<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
      String attrName = Attributes.Name.CLASS_PATH.toString();
      Attributes attrs = null;
      if (manifest == null || (attrs = manifest.getMainAttributes()) == null
          || !attrs.containsKey(attrName)) {
        return immutableSetOf();
      }
      Set<URI> uriSet = new LinkedHashSet<>();
      for (String path : split(attrs.getValue(attrName), " ")) {
        try {
          uriSet.add(getClassPathEntry(jarFile, path));
        } catch (URISyntaxException e) {
          logger.log(Level.WARNING, e, () -> "Invalid Class-Path entry: " + path);
        }
      }
      return uriSet;
    }

    protected void scan(URI uri, ClassLoader classloader) throws IOException {
      if (uri.getScheme().equals(FILE_SCHEMA) && scannedUris.add(uri)) {
        scanFromFile(new File(uri).getCanonicalFile(), classloader);
      } else if (uri.getScheme().equals(JAR_SCHEMA) && scannedUris.add(uri)) {
        scanFromJar(uri, classloader);
      } else {
        logger.warning(() -> "Invalid uri schema " + uri.getScheme());
      }
    }

    protected void scanDirectory(File directory, ClassLoader classLoader, Set<File> ancestors)
        throws IOException {
      String canonical = getRegularFilePath(directory);
      if (isBlank(root)) {
        scanDirectory(directory, classLoader, root, ancestors);
      } else {
        int clsFolderPos = canonical.indexOf(CLASSES_FOLDER);
        if (clsFolderPos != -1 && canonical.indexOf(root, clsFolderPos) != -1) {
          String pathPrefix = appendPathSeparatorIfNecessarily(root);
          scanDirectory(directory, classLoader, pathPrefix, ancestors);
        }
      }
    }

    protected void scanDirectory(File directory, ClassLoader classloader, String pathPrefix,
        Set<File> ancestors) throws IOException {
      if (!ancestors.add(directory)) {
        return;
      }
      File[] files = directory.listFiles();
      if (files == null) {
        logger.warning(() -> "Cannot read directory " + directory);
        return;
      }
      for (File file : files) {
        String name = file.getName();
        if (file.isDirectory()) {
          scanDirectory(file, classloader, pathPrefix + name + PATH_SEPARATOR, ancestors);
        } else {
          String resourceName = pathPrefix + name;
          if (!resourceName.equals(JarFile.MANIFEST_NAME) && filter.test(resourceName)) {
            resources.add(ClassPathResource.of(resourceName, classloader, file.toURI().toURL()));
          }
        }
      }
    }

    protected void scanFromFile(File file, ClassLoader classloader) throws IOException {
      if (!file.exists()) {
        return;
      }
      if (file.isDirectory()) {
        Set<File> ancestors = new LinkedHashSet<>();
        scanDirectory(file, classloader, ancestors);
        ancestors.clear();
      } else {
        scanSingleFile(file, classloader);
      }
    }

    protected void scanFromJar(URI uri, ClassLoader classloader) throws IOException {
      URI fileUri = tryExtractFileUri(uri);
      if (fileUri != null && scannedUris.add(fileUri)) {
        File jarFile = new File(fileUri).getCanonicalFile();
        if (jarFile.getCanonicalPath().toLowerCase(Locale.getDefault()).endsWith(WAR_EXT)) {
          scanWar(jarFile, classloader);
        } else {
          scanJar(uri, jarFile, classloader);
        }
      }
    }

    protected void scanJar(URI jarUri, File file, ClassLoader classloader) throws IOException {
      String jarPath = jarUri.toString();
      int sp = jarPath.indexOf(JAR_URL_SEPARATOR);
      if (sp != -1) {
        jarPath = jarPath.substring(0, sp);
      }
      JarFile jarFile;
      try {
        jarFile = new JarFile(file);
      } catch (IOException notJarFile) {
        logger.log(Level.WARNING, notJarFile,
            () -> String.format("The file %s is not jar file!", file.getName()));
        return;
      }
      try {
        for (URI uri : getClassPathFromManifest(file, jarFile.getManifest())) {
          scan(uri, classloader);
        }
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String resourceName = entry.getName();
          if (entry.isDirectory() || resourceName.equals(JarFile.MANIFEST_NAME)
              || isNotBlank(root) && !resourceName.startsWith(root) || !filter.test(resourceName)) {
            continue;
          }
          String resourceUrl = jarPath.concat(
              isNotBlank(resourceName) ? JAR_URL_SEPARATOR.concat(resourceName) : resourceName);
          resources.add(ClassPathResource.of(resourceName, classloader, new URL(resourceUrl)));
        }
      } finally {
        try {
          jarFile.close();
        } catch (IOException ignored) {
          // Noop!
        }
      }
    }

    protected void scanSingleFile(File file, ClassLoader classloader) throws IOException {
      String filePath = replace(file.getCanonicalPath(), File.separator, PATH_SEPARATOR_STRING);
      if (filePath.endsWith(JAR_EXT)) {
        scanJar(file.toURI(), file, classloader);
      } else if (!filePath.equals(JarFile.MANIFEST_NAME)) {
        int classesIdx = -1;
        if ((classesIdx = filePath.indexOf(CLASSES_FOLDER)) != -1) {
          filePath = filePath.substring(classesIdx + CLASSES_FOLDER.length());
        }
        if (filter.test(filePath)) {
          resources.add(ClassPathResource.of(filePath, classloader, file.toURI().toURL()));
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
        URI fileUri = new URI(jarUri.getRawSchemeSpecificPart());
        if (FILE_SCHEMA.equals(fileUri.getScheme())) {
          String fileUrlStr = fileUri.toString();
          int sp = fileUrlStr.indexOf(JAR_URL_SEPARATOR);
          if (sp != -1) {
            fileUrlStr = fileUrlStr.substring(0, sp);
          }
          return new URI(fileUrlStr);
        }
      } catch (URISyntaxException ignore) {
        logger.log(Level.WARNING, ignore,
            () -> String.format("Can not extract file uri from %s.", jarUri.toString()));
      }
      return null;
    }

    String appendPathSeparatorIfNecessarily(String path) {
      if (path == null) {
        return path;
      } else {
        return !path.endsWith(PATH_SEPARATOR_STRING) ? path + PATH_SEPARATOR_STRING : path;
      }
    }

    String getRegularFilePath(File file) throws IOException {
      return replace(file.getCanonicalPath(), File.separator, PATH_SEPARATOR_STRING);
    }

  }
}

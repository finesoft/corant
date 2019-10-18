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
import static org.corant.shared.util.StringUtils.defaultString;
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
import java.util.logging.Logger;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.PathUtils.GlobMatcher;
import org.corant.shared.util.PathUtils.GlobPatterns;
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
      immutableSetOf("java", "javax", "javafx", "jdk", "sun", "oracle", "netscape", "org/ietf",
          "org/jcp", "org/omg", "org/w3c", "org/xml", "com/sun", "com/oracle");
  private static final Logger logger = Logger.getLogger(ClassPaths.class.getName());

  private ClassPaths() {
    super();
  }

  /**
   * Build a class path resource scanner with path expression
   *
   * @param pathExpression the class path expression, may be glob expression.
   * @param ignoreCase
   * @return buildScanner
   */
  public static Scanner buildScanner(String pathExpression, boolean ignoreCase) {
    if (GlobMatcher.hasGlobChar(pathExpression)) {
      PathFilter pf = new PathFilter(ignoreCase, pathExpression);
      return new Scanner(pf.getRoot(), pf);
    } else {
      return new Scanner(pathExpression);
    }
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
   * Scan class path resource with path, path separator is '/', allowed for use glob-pattern.
   *
   * <pre>
   * for example:
   * 1.if path is "javax/sql" then will scan all resources that under the javax.sql class path.
   * 2.if path is "java/sql/Driver.class" then will scan single resource javax.sql.Driver.
   * 3.if path is "META-INF/maven" then will scan all resources under the META-INF/maven.
   * 4.if path is blank ({@code StringUtils.isBlank}) then will scan all class path in the system.
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filter class name
   * end with Driver.class.
   * </pre>
   *
   * @param classLoader
   * @param path
   * @return
   * @throws IOException from
   */
  public static Set<ClassPathResource> from(ClassLoader classLoader, String path)
      throws IOException {
    Scanner scanner = buildScanner(defaultString(path), false);
    for (Map.Entry<URI, ClassLoader> entry : getClassPathEntries(
        defaultObject(classLoader, defaultClassLoader()), scanner.getRoot()).entrySet()) {
      scanner.scan(entry.getKey(), entry.getValue());
    }
    return scanner.getResources();
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
            URLClassLoader currUrlClsLoader = URLClassLoader.class.cast(currClsLoader);
            for (URL entry : currUrlClsLoader.getURLs()) {
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
   * Use glob express for filtering.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static final class PathFilter extends GlobMatcher {

    protected PathFilter(boolean ignoreCase, String pathExpress) {
      super(false, ignoreCase, pathExpress);
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
        return immutableSetOf();
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
            resources.add(ClassPathResource.of(resourceName, classloader));
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
          resources.add(ClassPathResource.of(name, classloader));
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
          resources.add(ClassPathResource.of(resourceName, classloader));
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
        // String specPart = jarUri.getSchemeSpecificPart();
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
        logger.warning(() -> String.format("Can not extract file uri from %s.", jarUri.toString()));
      }
      return null;
    }
  }
}

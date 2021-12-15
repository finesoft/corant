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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Sets.immutableSetOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.replace;
import static org.corant.shared.util.Strings.split;
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
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.PathMatcher;

/**
 * corant-shared
 *
 * @author bingo 下午3:59:11
 *
 */
public class ClassPathResourceScanner {

  public static final char CLASS_PATH_SEPARATOR = '.';
  public static final char PATH_SEPARATOR = '/';
  public static final String PATH_SEPARATOR_STRING = "/";
  public static final String TOP_PATH = "..";
  public static final String JAR_URL_SEPARATOR = "!/";
  public static final String CLASSES = "classes";
  public static final String CLASSES_FOLDER = "classes/";
  public static final String JAR_EXT = ".jar";
  public static final String WAR_EXT = ".war";
  public static final String META_INF = "META-INF";
  public static final String LIB = "lib";
  public static final String WEB_INF = "WEB-INF";
  public static final String FILE_SCHEMA = "file";
  public static final String JAR_SCHEMA = "jar";
  public static final String JRT_SCHEMA = "jrt";// from JDK9

  protected static final Logger logger = Logger.getLogger(ClassPathResourceScanner.class.getName());
  protected static final Map<Path, URLClassLoader> cachedClassLoaders = new ConcurrentHashMap<>();// static?

  protected final Set<ClassPathResource> resources = new LinkedHashSet<>();
  protected final Set<URI> scannedUris = new HashSet<>();
  protected final String root;

  protected Predicate<String> filter = s -> true;

  public ClassPathResourceScanner(PathMatcher matcher) {
    this(matcher.getPlainParent(PATH_SEPARATOR_STRING), matcher);
  }

  public ClassPathResourceScanner(String root) {
    this.root = root;
  }

  public ClassPathResourceScanner(String root, Predicate<String> filter) {
    this(root);
    if (filter != null) {
      this.filter = filter;
    }
  }

  /**
   * Get the resources of a relative path through a class and path
   *
   * @param relative the class to load resources with
   * @param path relative or absolute path within the class path
   * @return the class path resource
   */
  public static URLResource relative(Class<?> relative, String path) {
    URL url = null;
    if (path != null) {
      if (relative != null) {
        url =
            relative.getResource(
                path.contains(TOP_PATH)
                    ? PATH_SEPARATOR_STRING.concat(Path
                        .of(relative.getCanonicalName().replace(CLASS_PATH_SEPARATOR,
                            PATH_SEPARATOR))
                        .getParent().resolve(path).normalize().toString()
                        .replace(File.separatorChar, PATH_SEPARATOR))
                    : path);
      } else {
        url = defaultClassLoader().getResource(path);
      }
    }
    return url != null ? new URLResource(url) : null;
  }

  /**
   * Build a war class loader, Careful use may result in leakage. We only extract WEB-INF folder to
   * default temporary-file directory.
   *
   * @param path the temporary-file path that used to place the war resources.
   * @param parentClassLoader the parent class loader
   * @see Files#createTempDirectory(String, java.nio.file.attribute.FileAttribute...)
   * @return The war class loader
   * @throws IOException If I/O errors occur
   */
  static synchronized URLClassLoader buildWarClassLoader(Path path,
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

  public Set<ClassPathResource> getResources() {
    return resources;
  }

  public String getRoot() {
    return root;
  }

  public ClassPathResourceScanner scan(URI uri, ClassLoader classloader) throws IOException {
    if (FILE_SCHEMA.equals(uri.getScheme())) {
      if (scannedUris.add(uri)) {
        scanFromFile(new File(uri), classloader);
      }
    } else if (JAR_SCHEMA.equals(uri.getScheme())) {
      if (scannedUris.add(uri)) {
        scanFromJar(uri, classloader);
      }
    } else {
      logger.warning(() -> "Invalid uri " + uri + ", schema " + uri.getScheme());
    }
    return this;
  }

  /**
   * Unfinish yet // FIXME
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
   */
  protected Set<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
    String attrName = Attributes.Name.CLASS_PATH.toString();
    Attributes attrs;
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
        if (!JarFile.MANIFEST_NAME.equals(resourceName) && filter.test(resourceName)) {
          resources.add(ClassPathResource.of(resourceName, classloader, file.toURI().toURL()));
        }
      }
    }
  }

  protected void scanFromFile(File file, ClassLoader classloader) throws IOException {
    File useFile = file;
    if (!useFile.exists()) {
      String filePath = useFile.getPath();
      while (filePath.endsWith("*")) {
        filePath = filePath.substring(0, filePath.length() - 1);
      }
      if (filePath.length() > 0) {
        useFile = new File(filePath);
      }
      if (!useFile.exists()) {
        return;
      }
    }
    useFile = useFile.getCanonicalFile();
    if (useFile.isDirectory()) {
      Set<File> ancestors = new LinkedHashSet<>();
      scanDirectory(useFile, classloader, ancestors);
      ancestors.clear();
    } else {
      scanSingleFile(useFile, classloader);
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
        if (entry.isDirectory() || JarFile.MANIFEST_NAME.equals(resourceName)
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
    } else if (!JarFile.MANIFEST_NAME.equals(filePath)) {
      int classesIdx;
      if ((classesIdx = filePath.indexOf(CLASSES_FOLDER)) != -1) {
        filePath = filePath.substring(classesIdx + CLASSES_FOLDER.length());
      }
      if (filter.test(filePath)) {
        resources.add(ClassPathResource.of(filePath, classloader, file.toURI().toURL()));
      }
    }
  }

  protected void scanWar(File file, ClassLoader parent) throws IOException {
    // We need to build a temporary war class loader;
    ClassPathResourceScanner warScanner = new ClassPathResourceScanner(root, filter);
    final URLClassLoader warClassLoader = buildWarClassLoader(file.toPath(), parent);
    for (URL url : warClassLoader.getURLs()) {
      try {
        warScanner.scan(url.toURI(), warClassLoader);
      } catch (URISyntaxException e) {
        throw new CorantRuntimeException(e);
      }
    }
    resources.addAll(warScanner.resources);
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
          () -> String.format("Can not extract file uri from %s.", jarUri));
    }
    return null;
  }

  String appendPathSeparatorIfNecessarily(String path) {
    if (path == null) {
      return null;
    } else {
      return path.endsWith(PATH_SEPARATOR_STRING) ? path : path + PATH_SEPARATOR_STRING;
    }
  }

  String getRegularFilePath(File file) throws IOException {
    return replace(file.getCanonicalPath(), File.separator, PATH_SEPARATOR_STRING);
  }

}

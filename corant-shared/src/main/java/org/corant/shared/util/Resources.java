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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.MapUtils.immutableMapOf;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午1:16:04
 *
 */
public class Resources {

  public static final Logger logger = Logger.getLogger(Resources.class.getName());

  public static <T extends Resource> Stream<T> from(String path) throws IOException {
    if (isNotBlank(path)) {
      Optional<SourceType> ost = SourceType.decide(path);
      if (ost.isPresent()) {
        SourceType st = ost.get();
        if (st == SourceType.FILE_SYSTEM) {
          return forceCast(fromFileSystem(path));
        } else if (st == SourceType.URL) {
          return forceCast(fromUrl(path));
        } else {
          // default
          return forceCast(fromClassPath(path));
        }
      }
    }
    return Stream.empty();
  }

  /**
   * Use specified class loader to scan all class path resources.
   *
   * @param classLoader
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(ClassLoader classLoader)
      throws IOException {
    return fromClassPath(classLoader, null);
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
   * @param classPath
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(ClassLoader classLoader, String classPath)
      throws IOException {
    return ClassPaths.from(classLoader, SourceType.CLASS_PATH.resolve(classPath)).stream();
  }

  /**
   * Use default class loader to scan the specified path resources.
   *
   * @param classPath
   * @return
   * @throws IOException fromClassPath
   */
  public static Stream<ClassPathResource> fromClassPath(String classPath) throws IOException {
    return fromClassPath(defaultClassLoader(), SourceType.CLASS_PATH.resolve(classPath));
  }

  /**
   * Use file create file system resource.
   *
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(File file) throws IOException {
    return new FileSystemResource(shouldNotNull(file));
  }

  /**
   * Use Path to find file system resource.
   *
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(Path path) throws IOException {
    return new FileSystemResource(shouldNotNull(path).toFile());
  }

  /**
   * Use path string to find file system resource.
   *
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(String path) throws IOException {
    return new FileSystemResource(shouldNotNull(SourceType.FILE_SYSTEM.resolve(path)));
  }

  /**
   * Use input stream to build input stream resource.
   *
   * @param inputStream
   * @param location
   * @return
   * @throws IOException fromInputStream
   */
  public static InputStreamResource fromInputStream(InputStream inputStream, String location)
      throws IOException {
    return new InputStreamResource(location, inputStream);
  }

  /**
   * Use specified URL string to find resource.
   *
   * @param url
   * @return
   * @throws IOException fromUrl
   */
  public static URLResource fromUrl(String url) throws IOException {
    return new URLResource(SourceType.URL.resolve(url));
  }

  /**
   * Use specified URL to find resource.
   *
   * @param url
   * @return
   * @throws IOException fromUrl
   */
  public static URLResource fromUrl(URL url) throws IOException {
    return new URLResource(url);
  }

  /**
   * Use specified http URL and proxy to find resource.
   *
   * @param url
   * @param proxy
   * @return
   * @throws IOException fromUrl
   */
  public static InputStreamResource fromUrl(URL url, Proxy proxy) throws IOException {
    return fromInputStream(url.openConnection(proxy).getInputStream(), url.toExternalForm());
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see from
   *
   * @param path
   * @return tryFrom
   */
  public static <T extends Resource> Stream<T> tryFrom(final String path) {
    try {
      return from(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(ClassLoader)
   * @param classLoader
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(ClassLoader classLoader) {
    try {
      return fromClassPath(classLoader);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from class loader %s", classLoader));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(ClassLoader, String)
   * @param classLoader
   * @param classPath
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(ClassLoader classLoader,
      String classPath) {
    try {
      return fromClassPath(classLoader, classPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String
          .format("Can not find resource from class loader %s, path %s", classLoader, classPath));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromClassPath(String)
   * @param classPath
   * @return tryFromClassPath
   */
  public static Stream<ClassPathResource> tryFromClassPath(String classPath) {
    try {
      return fromClassPath(classPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from path %s", classPath));
    }
    return Stream.empty();
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromFileSystem(Path)
   * @param path
   * @return tryFromFileSystem
   */
  public static FileSystemResource tryFromFileSystem(Path path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromFileSystem(String)
   * @param path
   * @return tryFromFileSystem
   */
  public static FileSystemResource tryFromFileSystem(String path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find resource from path %s", path));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromInputStream(InputStream, String)
   * @param inputStream
   * @param location
   * @return tryFromInputStream
   */
  public static InputStreamResource tryFromInputStream(InputStream inputStream, String location) {
    try {
      return fromInputStream(inputStream, location);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> "Can not find resource from input stream");
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(String)
   * @param url
   * @return tryFromUrl
   */
  public static URLResource tryFromUrl(String url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(URL)
   * @param url
   * @return tryFromUrl
   */
  public static URLResource tryFromUrl(URL url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #fromUrl(URL, Proxy)
   * @param url
   * @param proxy
   * @return tryFromUrl
   */
  public static InputStreamResource tryFromUrl(URL url, Proxy proxy) {
    try {
      return fromUrl(url, proxy);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s", url));
    }
    return null;
  }

  /**
   * corant-shared
   *
   * Describe class path resource include class resource.
   *
   * @author bingo 下午2:04:58
   *
   */
  public static class ClassPathResource implements Resource {
    final ClassLoader classLoader;
    final String location;
    final SourceType sourceType = SourceType.CLASS_PATH;

    public ClassPathResource(String location, ClassLoader classLoader) {
      this.location = shouldNotNull(location);
      this.classLoader = shouldNotNull(classLoader);
    }

    public static ClassPathResource of(String location, ClassLoader classLoader) {
      if (location.endsWith(ClassUtils.CLASS_FILE_NAME_EXTENSION)) {
        return new ClassResource(location, classLoader);
      } else {
        return new ClassPathResource(location, classLoader);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ClassPathResource other = (ClassPathResource) obj;
      if (classLoader == null) {
        if (other.classLoader != null) {
          return false;
        }
      } else if (!classLoader.equals(other.classLoader)) {
        return false;
      }
      if (location == null) {
        if (other.location != null) {
          return false;
        }
      } else if (!location.equals(other.location)) {
        return false;
      }
      return true;
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    @Override
    public String getLocation() {
      return location;
    }

    public final String getResourceName() {
      return location;
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    public final URL getUrl() {
      return classLoader.getResource(location);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (classLoader == null ? 0 : classLoader.hashCode());
      result = prime * result + (location == null ? 0 : location.hashCode());
      return result;
    }

    @Override
    public final InputStream openStream() throws IOException {
      URLConnection conn = getUrl().openConnection();
      if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("window")) {
        conn.setUseCaches(false);
      }
      return conn.getInputStream();
    }

  }

  /**
   * corant-shared
   *
   * Describe class resource, but doesn't load it right away.
   *
   * @author bingo 下午2:04:09
   *
   */
  public static class ClassResource extends ClassPathResource {

    final String className;

    public ClassResource(String name, ClassLoader classLoader) {
      super(name, classLoader);
      int classNameEnd = name.length() - ClassUtils.CLASS_FILE_NAME_EXTENSION.length();
      className = name.substring(0, classNameEnd).replace(ClassPaths.PATH_SEPARATOR,
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
      ClassResource other = (ClassResource) obj;
      if (className == null) {
        if (other.className != null) {
          return false;
        }
      } else if (!className.equals(other.className)) {
        return false;
      }
      return true;
    }

    public String getClassName() {
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
        return classLoader.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

  }

  public static class FileSystemResource implements Resource {
    final SourceType sourceType = SourceType.FILE_SYSTEM;
    final File file;

    public FileSystemResource(File file) {
      this.file = shouldNotNull(file);
    }

    public FileSystemResource(String location) {
      super();
      file = new File(shouldNotNull(location));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      FileSystemResource other = (FileSystemResource) obj;
      if (file == null) {
        if (other.file != null) {
          return false;
        }
      } else if (!file.equals(other.file)) {
        return false;
      }
      return true;
    }

    public File getFile() {
      return file;
    }

    @Override
    public String getLocation() {
      try {
        return file.getCanonicalPath();
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }

    @Override
    public Map<String, Object> getMetadatas() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType(), "path",
          getFile().getPath(), "fileName", getFile().getName(), "lastModified",
          getFile().lastModified(), "length", getFile().length());
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (file == null ? 0 : file.hashCode());
      return result;
    }

    @Override
    public InputStream openStream() throws IOException {
      return new FileInputStream(file);
    }

  }

  public static class InputStreamResource implements Resource {
    final String location;
    final SourceType sourceType = SourceType.UNKNOW;
    final InputStream inputStream;

    public InputStreamResource(String location, InputStream inputStream) {
      super();
      this.location = location;
      this.inputStream = inputStream;
    }

    public InputStreamResource(URL url) throws MalformedURLException, IOException {
      location = url.toExternalForm();
      inputStream = url.openStream();
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    @Override
    public InputStream openStream() throws IOException {
      return inputStream;
    }

  }

  public interface Resource {

    String getLocation();

    default Map<String, Object> getMetadatas() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType());
    }

    SourceType getSourceType();

    InputStream openStream() throws IOException;
  }

  public enum SourceType {

    FILE_SYSTEM("filesystem:"), CLASS_PATH("classpath:"), URL("url:"), UNKNOW("unknow:");

    private final String prefix;
    private final int prefixLength;

    private SourceType(String prefix) {
      this.prefix = prefix;
      prefixLength = prefix.length();
    }

    public static Optional<SourceType> decide(String path) {
      SourceType ps = null;
      if (isNotBlank(path)) {
        for (SourceType p : SourceType.values()) {
          if (p.match(path) && path.length() > p.getPrefixLength()) {
            ps = p;
            break;
          }
        }
      }
      return Optional.ofNullable(ps);
    }

    public String getPrefix() {
      return prefix;
    }

    public int getPrefixLength() {
      return prefixLength;
    }

    public boolean match(String path) {
      if (isBlank(path)) {
        return false;
      }
      return path.startsWith(prefix);
    }

    public String regulate(String path) {
      if (path != null && !path.startsWith(getPrefix())) {
        return getPrefix() + path;
      }
      return path;
    }

    public String resolve(String path) {
      if (path != null && path.startsWith(getPrefix())) {
        return path.substring(getPrefixLength());
      }
      return path;
    }
  }

  public static class URLResource implements Resource {
    final String location;
    final SourceType sourceType = SourceType.URL;
    final URL url;

    public URLResource(String url) throws MalformedURLException {
      this(new URL(url));
    }

    public URLResource(URL url) {
      super();
      this.url = url;
      location = url.toExternalForm();
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public Map<String, Object> getMetadatas() {
      return immutableMapOf("location", getLocation(), "sourceType", getSourceType(), "url",
          url.toExternalForm());
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    @Override
    public InputStream openStream() throws IOException {
      return url.openStream();
    }

  }
}

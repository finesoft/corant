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
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Maps.immutableMapOf;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.SLASH;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
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

  /**
   * Get URL resources from specified URL path, support Glob / Regex Pattern.
   * <p>
   * <ul>
   * <li>The incoming path start with 'filesystem:'({@link SourceType#FILE_SYSTEM}) means that get
   * resources from file system.</li>
   * <li>The incoming path start with 'classpath:' ({@link SourceType#CLASS_PATH}) means that get
   * resources from class path.</li>
   * <li>The incoming path start with 'url:' ({@link SourceType#CLASS_PATH}) means that get
   * resources from URL.</li>
   * </ul>
   * Note: If the incoming path non start with {@link SourceType} that means not specified schema
   * then use class path.
   *
   * @param <T> the resource type
   * @param path the resource path
   * @throws IOException
   * @see PathMatcher#decidePathMatcher(String, boolean, boolean)
   */
  public static <T extends URLResource> Stream<T> from(String path) throws IOException {
    if (isNotBlank(path)) {
      SourceType st = SourceType.decide(path).orElse(SourceType.CLASS_PATH);
      if (st == SourceType.FILE_SYSTEM) {
        return forceCast(fromFileSystem(path));
      } else if (st == SourceType.URL) {
        return forceCast(streamOf(fromUrl(path)));
      } else {
        return forceCast(fromClassPath(path));// default from class path;
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
   * 4.if path is blank ({@code Strings.isBlank}) then will scan all class path in the system.
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
   * @param file
   * @return
   * @throws IOException fromFileSystem
   */
  public static FileSystemResource fromFileSystem(File file) {
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
   * Use path string to find file system resource. Support glob/regex expression
   *
   * @see PathMatcher#decidePathMatcher(String, boolean, boolean)
   * @param path
   * @return
   * @throws IOException fromFileSystem
   */
  public static Stream<FileSystemResource> fromFileSystem(String path) throws IOException {
    String pathExp = SourceType.FILE_SYSTEM.resolve(path);
    return FileUtils.searchFiles(pathExp).stream().map(FileSystemResource::new);
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
    return new InputStreamResource(inputStream, location, null);
  }

  /**
   * Get the resources of a relative path through a class and path
   *
   * @param relative
   * @param path
   * @return fromRelativeClass
   */
  public static URLResource fromRelativeClass(Class<?> relative, String path) {
    return ClassPaths.fromRelative(relative, path);
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
   * @see #from
   *
   * @param path
   * @return tryFrom
   */
  public static <T extends URLResource> Stream<T> tryFrom(final String path) {
    try {
      return from(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from path %s.", path));
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
          () -> String.format("Can not find resource from class loader %s.", classLoader));
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
          .format("Can not find resource from class loader %s, path %s.", classLoader, classPath));
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
          () -> String.format("Can not find resource from path %s.", classPath));
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
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from path %s.", path));
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
  public static Stream<FileSystemResource> tryFromFileSystem(String path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Can not find resource from path %s.", path));
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
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s.", url));
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
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s.", url));
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
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s.", url));
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
  public static class ClassPathResource extends URLResource {

    final ClassLoader classLoader;
    final String classPath;

    public ClassPathResource(String classPath, ClassLoader classLoader, URL url) {
      super(url, SourceType.CLASS_PATH);
      this.classLoader = shouldNotNull(classLoader);
      this.classPath = shouldNotNull(classPath);
    }

    public static ClassPathResource of(String classPath, ClassLoader classLoader, URL url) {
      if (classPath.endsWith(Classes.CLASS_FILE_NAME_EXTENSION)) {
        return new ClassResource(classPath, classLoader, url);
      } else {
        return new ClassPathResource(classPath, classLoader, url);
      }
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
      ClassPathResource other = (ClassPathResource) obj;
      if (classLoader == null) {
        if (other.classLoader != null) {
          return false;
        }
      } else if (!classLoader.equals(other.classLoader)) {
        return false;
      }
      if (classPath == null) {
        return other.classPath == null;
      } else {
        return classPath.equals(other.classPath);
      }
    }

    public ClassLoader getClassLoader() {
      return classLoader;
    }

    public String getClassPath() {
      return classPath;
    }

    @Override
    public String getLocation() {
      return classPath;
    }

    /*
     * @Override public final URL getURL() { if (url == null) { synchronized (this) { if (url ==
     * null) { url = classLoader.getResource(classPath); } } } return url; }
     */

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (classLoader == null ? 0 : classLoader.hashCode());
      result = prime * result + (classPath == null ? 0 : classPath.hashCode());
      return result;
    }

    @Override
    public final InputStream openStream() throws IOException {
      URLConnection conn = getURL().openConnection();
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

    public ClassResource(String classPath, ClassLoader classLoader, URL url) {
      super(classPath, classLoader, url);
      String useClassPath = classPath;
      if (useClassPath.indexOf(ClassPaths.JAR_URL_SEPARATOR) != -1) {
        useClassPath = useClassPath.substring(classPath.indexOf(ClassPaths.JAR_URL_SEPARATOR)
            + ClassPaths.JAR_URL_SEPARATOR.length());
      }
      if (useClassPath.indexOf(ClassPaths.CLASSES_FOLDER) != -1) {
        useClassPath = useClassPath.substring(
            useClassPath.indexOf(ClassPaths.CLASSES_FOLDER) + ClassPaths.CLASSES_FOLDER.length());
      }
      int classNameEnd = useClassPath.length() - Classes.CLASS_FILE_NAME_EXTENSION.length();
      className = useClassPath.substring(0, classNameEnd).replace(ClassPaths.PATH_SEPARATOR,
          Classes.PACKAGE_SEPARATOR_CHAR);
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
        return other.className == null;
      } else {
        return className.equals(other.className);
      }
    }

    public String getClassName() {
      return className;
    }

    public String getPackageName() {
      return Classes.getPackageName(className);
    }

    public String getSimpleName() {
      return Classes.getShortClassName(className);
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

  /**
   * corant-shared
   *
   * Describe system file
   *
   * @author bingo 下午6:53:19
   *
   */
  public static class FileSystemResource extends URLResource {

    final File file;

    public FileSystemResource(File file) {
      super(getFileUrl(file), SourceType.FILE_SYSTEM);
      this.file = shouldNotNull(file);
    }

    public FileSystemResource(String location) {
      this(new File(shouldNotNull(location)));
    }

    private static URL getFileUrl(File file) {
      try {
        return shouldNotNull(file).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new CorantRuntimeException(e);
      }
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
      FileSystemResource other = (FileSystemResource) obj;
      if (file == null) {
        return other.file == null;
      } else {
        return file.equals(other.file);
      }
    }

    public File getFile() {
      return file;
    }

    @Override
    public String getLocation() {
      return file.getAbsolutePath();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType", SourceType.FILE_SYSTEM.name(),
          "path", file.getPath(), "fileName", getName(), "lastModified", file.lastModified(),
          "length", file.length());
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (file == null ? 0 : file.hashCode());
      return result;
    }

    @Override
    public InputStream openStream() throws IOException {
      return new FileInputStream(file);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> cls) {
      if (File.class.isAssignableFrom(cls)) {
        return (T) file;
      }
      throw new IllegalArgumentException("Can't unwrap resource to " + cls);
    }
  }

  /**
   * corant-shared
   *
   * Describe input stream resource, can specify a specific name.
   *
   * @author bingo 下午6:54:04
   *
   */
  public static class InputStreamResource implements Resource {
    final String name;
    final String location;
    final SourceType sourceType = SourceType.UNKNOWN;
    final InputStream inputStream;

    /**
     * @param inputStream
     * @param name
     */
    public InputStreamResource(InputStream inputStream, String name) {
      this.name = name;
      this.inputStream = inputStream;
      location = null;
    }

    public InputStreamResource(InputStream inputStream, String location, String name) {
      this.name = name;
      this.location = location;
      this.inputStream = inputStream;
    }

    /**
     *
     * @param url
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStreamResource(URL url) throws IOException {
      location = url.toExternalForm();
      inputStream = url.openStream();
      name = url.getFile();
    }

    @Override
    public String getLocation() {
      return location;
    }

    @Override
    public String getName() {
      return name;
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

  /**
   * corant-shared
   *
   * <p>
   * Object that representation of a resource that can be loaded from URL, class, file system or
   * inputstream.
   * </p>
   *
   * @author bingo 下午3:19:30
   *
   */
  public interface Resource {

    String META_CONTENT_TYPE = "Content-Type";
    String META_CONTENT_LENGTH = "Content-Length";
    String META_LAST_MODIFIED = "Last-Modified";

    /**
     * Return a byte array for the content of this resource, please evaluate the size of the
     * resource when using it to avoid OOM.
     *
     * NOTE: the stream will be closed after reading.
     *
     * @throws IOException If I/O errors occur
     */
    default byte[] getBytes() throws IOException {
      try (InputStream is = openStream()) {
        return Streams.readAllBytes(is);
      }
    }

    /**
     * Return the location of this resource, depends on original source. Depending on source type,
     * this may be:
     * <ul>
     * <li>FILE_SYSTEM - absolute path to the file</li>
     * <li>CLASS_PATH - class resource path</li>
     * <li>URL - string of the URI</li>
     * <li>UNKNOWN - whatever location was provided to {@link InputStreamResource}</li>
     * </ul>
     */
    String getLocation();

    /**
     * Return the meta information of this resource. For example: author, date created and date
     * modified,size etc.
     *
     * @return getMetadata
     */
    default Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType",
          getSourceType() == null ? null : getSourceType().name(), "name", getName());
    }

    /**
     * The name of this resource. this may be:
     * <ul>
     * <li>FILE_SYSTEM - the underlying file name</li>
     * <li>CLASS_PATH - the underlying class path resource name</li>
     * <li>URL - the file name of this URL {@link URL#getFile()}</li>
     * <li>UNKNOWN - whatever name was provided to {@link InputStreamResource}</li>
     * </ul>
     *
     * @return getName
     */
    default String getName() {
      return null;
    }

    /**
     * Return the original source type
     *
     * @return getSourceType
     */
    SourceType getSourceType();

    /**
     * Return an {@link InputStream} for the content of this resource
     *
     * @return
     * @throws IOException openStream
     */
    InputStream openStream() throws IOException;

    /**
     * Return an {@link InputStream} for the content of the resource, do not throw any exceptions.
     *
     * @return tryOpenStream
     */
    default InputStream tryOpenStream() {
      try {
        return openStream();
      } catch (IOException e) {
        logger.log(Level.WARNING, e,
            () -> String.format("Can't not open stream from %s.", getLocation()));
      }
      return null;
    }
  }

  /**
   * corant-shared
   *
   * <p>
   * Object that representation of a original source of a Resource
   * </p>
   *
   * @author bingo 下午3:31:29
   *
   */
  public enum SourceType {

    /**
     * load resource from file system
     */
    FILE_SYSTEM("filesystem:"),

    /**
     * load resource from class path
     */
    CLASS_PATH("classpath:"),

    /**
     * load resource from URL
     */
    URL("url:"),

    /**
     * load resource from input stream
     */
    UNKNOWN("unknown:");

    private final String prefix;
    private final int prefixLength;

    SourceType(String prefix) {
      this.prefix = prefix;
      prefixLength = prefix.length();
    }

    public static Optional<SourceType> decide(String path) {
      SourceType ps = null;
      if (isNotBlank(path)) {
        for (SourceType p : SourceType.values()) {
          if (p.match(path) && path.length() > p.prefixLength) {
            ps = p;
            break;
          }
        }
      }
      return Optional.ofNullable(ps);
    }

    public static String decideSeparator(String path) {
      return decide(path).orElse(UNKNOWN).getSeparator();
    }

    public String getPrefix() {
      return prefix;
    }

    public int getPrefixLength() {
      return prefixLength;
    }

    public String getSeparator() {
      if (this == CLASS_PATH || this == URL) {
        return SLASH;
      } else if (this == FILE_SYSTEM) {
        return File.separator;
      } else {
        return EMPTY;
      }
    }

    public boolean match(String path) {
      if (isBlank(path)) {
        return false;
      }
      return path.startsWith(prefix);
    }

    public String regulate(String path) {
      if (path != null && !path.startsWith(prefix)) {
        return prefix + path;
      }
      return path;
    }

    public String resolve(String path) {
      if (path != null && path.startsWith(prefix)) {
        return path.substring(prefixLength);
      }
      return path;
    }
  }

  /**
   * corant-shared
   * <p>
   * A representation of a resource that load from URL
   *
   * @author bingo 下午4:06:15
   *
   */
  public static class URLResource implements WrappedResource {
    final SourceType sourceType;
    final URL url;

    public URLResource(String url) throws MalformedURLException {
      this(new URL(url), SourceType.URL);
    }

    public URLResource(URL url) {
      this(url, SourceType.URL);
    }

    URLResource(URL url, SourceType sourceType) {
      this.url = shouldNotNull(url);
      this.sourceType = shouldNotNull(sourceType);
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
      URLResource other = (URLResource) obj;
      return getURI().equals(other.getURI()); // FIXME URI hq
    }

    @Override
    public String getLocation() {
      return url.toExternalForm();
    }

    @Override
    public Map<String, Object> getMetadata() {
      return immutableMapOf("location", getLocation(), "sourceType",
          sourceType == null ? null : sourceType.name(), "url", url.toExternalForm());
    }

    @Override
    public String getName() {
      return FileUtils.getFileName(url.getPath());
    }

    @Override
    public SourceType getSourceType() {
      return sourceType;
    }

    public URI getURI() {
      try {
        return url.toURI();
      } catch (URISyntaxException e) {
        throw new CorantRuntimeException(e);
      }
    }

    public URL getURL() {
      return url;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getURI().hashCode(); // FIXME URI hq
      return result;
    }

    @Override
    public InputStream openStream() throws IOException {
      return url.openStream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> cls) {
      if (URL.class.isAssignableFrom(cls)) {
        return (T) url;
      }
      throw new IllegalArgumentException("Can't unwrap resource to " + cls);
    }
  }

  public interface WrappedResource extends Resource {
    <T> T unwrap(Class<T> cls);
  }
}

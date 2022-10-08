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
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.resource.ClassPathResource;
import org.corant.shared.resource.ClassPathResourceLoader;
import org.corant.shared.resource.FileSystemResource;
import org.corant.shared.resource.FileSystemResourceLoader;
import org.corant.shared.resource.InputStreamResource;
import org.corant.shared.resource.SourceType;
import org.corant.shared.resource.URLResource;

/**
 * corant-shared
 *
 * @author bingo 下午1:16:04
 *
 */
public class Resources {

  static final Logger logger = Logger.getLogger(Resources.class.getName());

  /**
   * Get URL resources from specified URL path, support Glob / Regex Pattern.
   * <p>
   * <ul>
   * <li>The incoming path start with 'filesystem:'({@link SourceType#FILE_SYSTEM}) means that get
   * resources from file system.</li>
   * <li>The incoming path start with 'classpath:' ({@link SourceType#CLASS_PATH}) means that get
   * resources from class path.</li>
   * <li>The incoming path start with 'url:' ({@link SourceType#URL}) means that get resources from
   * URL.</li>
   * </ul>
   * Note: If the incoming path non start with {@link SourceType} that means not specified schema
   * then use class path.
   *
   * @param <T> the resource type
   * @param path the resource path
   * @throws IOException if an I/O error occurs
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
   * @param classLoader class loader for loading resources
   * @return A class path resource stream
   * @throws IOException if an I/O error occurs
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
   * 4.if path is blank ({@code
   * Strings.isBlank
   * }) then will scan all class path in the system.
   * 5.if path is "javax/sql/*Driver.class" then will scan javax.sql class path and filter class name
   * end with Driver.class.
   * </pre>
   *
   * @param classLoader class loader for loading resources
   * @param classPath the resource class path or expression, supports glob-pattern/regex
   * @return A class path resource stream
   * @throws IOException if an I/O error occurs
   */
  public static Stream<ClassPathResource> fromClassPath(ClassLoader classLoader, String classPath)
      throws IOException {
    return new ClassPathResourceLoader(classLoader).load(classPath).stream();
  }

  /**
   * Use default class loader to scan the specified path resources.
   *
   * @param classPath the resource class path or expression, supports glob-pattern/regex
   * @return A class path resource stream
   * @throws IOException if an I/O error occurs
   */
  public static Stream<ClassPathResource> fromClassPath(String classPath) throws IOException {
    return ClassPathResourceLoader.DFLT_INST.load(classPath).stream();
  }

  /**
   * Use file create file system resource.
   *
   * @param file used to build FileSystemResource
   * @return A file system resource
   */
  public static FileSystemResource fromFileSystem(File file) {
    return new FileSystemResource(shouldNotNull(file));
  }

  /**
   * Use Path to find file system resource.
   *
   * @param path used to build FileSystemResource
   * @return A file system resource
   * @throws IOException if an I/O error occurs
   */
  public static FileSystemResource fromFileSystem(Path path) throws IOException {
    return new FileSystemResource(path);
  }

  /**
   * Use path string to find file system resource. Support glob/regex expression
   *
   * @param path the resource path or expression, supports glob-pattern/regex
   * @return A file system resource stream
   * @throws IOException if an I/O error occurs
   *
   * @see PathMatcher#decidePathMatcher(String, boolean, boolean)
   */
  public static Stream<FileSystemResource> fromFileSystem(String path) throws IOException {
    return FileSystemResourceLoader.DFLT_INST.load(path).stream();
  }

  /**
   * Use input stream to build input stream resource.
   *
   * @param inputStream used to build InputStreamResource
   * @param location used to denote the origin of a given input stream
   * @return an input stream resource
   * @throws IOException if an I/O error occurs
   */
  public static InputStreamResource fromInputStream(InputStream inputStream, String location)
      throws IOException {
    return new InputStreamResource(inputStream, location, null);
  }

  /**
   * Get the resources of a relative path through a class and path
   *
   * @param relative the relative class use to search the resource
   * @param path resource class path
   * @return a URLResource
   */
  public static URLResource fromRelativeClass(Class<?> relative, String path) {
    return ClassPathResourceLoader.relative(relative, path);
  }

  /**
   * Use specified URL string to find resource.
   *
   * @param url used to build URLResource
   * @return a URLResource
   * @throws IOException if an I/O error occurs
   */
  public static URLResource fromUrl(String url) throws IOException {
    return new URLResource(SourceType.URL.resolve(url));
  }

  /**
   * Use specified URL to find resource.
   *
   * @param url used to build URLResource
   * @return a URLResource
   * @throws IOException if an I/O error occurs
   */
  public static URLResource fromUrl(URL url) throws IOException {
    return new URLResource(url);
  }

  /**
   * Use specified http URL and proxy to find resource.
   *
   * @param url used to build URLResource
   * @param proxy the Proxy through which this connection will be made
   * @return an input stream resource
   * @throws IOException if an I/O error occurs
   */
  public static InputStreamResource fromUrl(URL url, Proxy proxy) throws IOException {
    return fromInputStream(url.openConnection(proxy).getInputStream(), url.toExternalForm());
  }

  /**
   * Not throw IO exception, just warning
   *
   * @see #from
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
   */
  public static InputStreamResource tryFromUrl(URL url, Proxy proxy) {
    try {
      return fromUrl(url, proxy);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> String.format("Can not find url resource from %s.", url));
    }
    return null;
  }
}

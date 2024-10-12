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

import static java.lang.String.format;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
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
 */
public class Resources {

  static final Logger logger = Logger.getLogger(Resources.class.getName());

  /**
   * Find all URL resources according to the given path, the path support Glob / Regex Pattern.
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
   * Find all class path resources according to the given class loader, throws exception if error
   * occurs.
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
   *
   * Find all class path resources according to the given class loader and class path, throws
   * exception if error occurs.
   * <p>
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
   * 4.if path is blank (Strings.isBlank) then will scan all class path in the system.
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
   * Find all class path resources according to the given class path and default class loader,
   * throws exception if error occurs.
   *
   * @param classPath the resource class path or expression, supports glob-pattern/regex
   * @return A class path resource stream
   * @throws IOException if an I/O error occurs
   */
  public static Stream<ClassPathResource> fromClassPath(String classPath) throws IOException {
    return ClassPathResourceLoader.DFLT_INST.load(classPath).stream();
  }

  /**
   * Returns a file system resource according to the given file.
   *
   * @param file used to build FileSystemResource, can't null
   * @return A file system resource
   */
  public static FileSystemResource fromFileSystem(File file) {
    return new FileSystemResource(file);
  }

  /**
   * Returns a file system resource according to the given path.
   *
   * @param path used to build FileSystemResource , can't null
   * @return A file system resource
   * @throws IOException if an I/O error occurs
   */
  public static FileSystemResource fromFileSystem(Path path) throws IOException {
    return new FileSystemResource(path);
  }

  /**
   * Find all file system resources according to the given path. the path support glob/regex
   * expression.
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
   * Returns an input stream resource according to the given input stream and optional location.
   *
   * @param inputStream used to build InputStreamResource, can't null
   * @param location used to denote the origin of a given input stream
   * @return an input stream resource
   */
  public static InputStreamResource fromInputStream(InputStream inputStream, String location) {
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
   * Returns a URL resource according to the given URL string.
   *
   * @param url used to build URLResource
   * @return a URLResource
   * @throws IOException if an I/O error occurs
   */
  public static URLResource fromUrl(String url) throws IOException {
    return new URLResource(SourceType.URL.resolve(url));
  }

  /**
   * Returns a URL resource according to the given URL.
   *
   * @param url used to build URLResource
   * @return a URLResource
   * @throws IOException if an I/O error occurs
   */
  public static URLResource fromUrl(URL url) throws IOException {
    return new URLResource(url);
  }

  /**
   * Returns an input stream resource according to the given HTTP URL and proxy
   *
   * @param url used to build URLResource
   * @param proxy the proxy through which this connection will be made
   * @return an input stream resource
   * @throws IOException if an I/O error occurs
   */
  public static InputStreamResource fromUrl(URL url, Proxy proxy) throws IOException {
    return fromInputStream(url.openConnection(proxy).getInputStream(), url.toExternalForm());
  }

  /**
   * Resolve and return a resource from the given path, throws exception if resource not exists or
   * any error occurs.
   * <p>
   * Note: if the path contains multiple resources return the first one.
   *
   * @param <T> the returned resource type
   * @param path the resource path, support Glob / Regex Pattern.
   *
   * @see #from(String)
   */
  public static <T extends URLResource> T resolve(String path) {
    try (Stream<T> stream = from(path)) {
      return stream.min(Comparator.comparing(URLResource::getURI)).orElseThrow(
          () -> new CorantRuntimeException("Can not find resource from path %s", path));
    } catch (Exception ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  /**
   * Try to get a URL resources from specified URL path, support Glob / Regex Pattern, not throw IO
   * exception, just warning.
   *
   * @param <T> the returned resource type
   * @param path the resource path
   * @see #from
   */
  public static <T extends URLResource> Stream<T> tryFrom(final String path) {
    try {
      return from(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find resource from path %s.", path));
    }
    return Stream.empty();
  }

  /**
   * Try to get all class path resources by the specified class loader, not throw IO exception, just
   * warning.
   *
   * @param classLoader class loader for loading resources
   * @see #fromClassPath(ClassLoader)
   */
  public static Stream<ClassPathResource> tryFromClassPath(ClassLoader classLoader) {
    try {
      return fromClassPath(classLoader);
    } catch (IOException e) {
      logger.log(Level.WARNING, e,
          () -> format("Can not find resource from class loader %s.", classLoader));
    }
    return Stream.empty();
  }

  /**
   * Try to get all class path resources by the specified class loader and path, not throw IO
   * exception, just warning.
   *
   * @param classLoader class loader for loading resources
   * @param classPath the resource class path or expression, supports glob-pattern/regex
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
   * Try to get all class path resources by the specified class path, not throw IO exception, just
   * warning.
   *
   * @param classPath the resource class path or expression, supports glob-pattern/regex
   * @return A class path resource stream
   * @see #fromClassPath(String)
   */
  public static Stream<ClassPathResource> tryFromClassPath(String classPath) {
    try {
      return fromClassPath(classPath);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find resource from path %s.", classPath));
    }
    return Stream.empty();
  }

  /**
   * Try to find a file system resource, not throw IO exception, just warning.
   *
   * @param path used to build FileSystemResource
   * @return A file system resource or null if exception occurred
   *
   * @see #fromFileSystem(Path)
   */
  public static FileSystemResource tryFromFileSystem(Path path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find resource from path %s.", path));
    }
    return null;
  }

  /**
   * Try to find all file system resources according to the given path. the path support glob/regex
   * expression, not throw IO exception, just warning.
   *
   * @param path the resource path or expression, supports glob-pattern/regex
   * @see #fromFileSystem(String)
   */
  public static Stream<FileSystemResource> tryFromFileSystem(String path) {
    try {
      return fromFileSystem(path);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find resource from path %s.", path));
    }
    return Stream.empty();
  }

  /**
   * Try to return an input stream resource according to the given input stream and optional
   * location or null if any error occurs.
   *
   * @param inputStream used to build InputStreamResource, can't null
   * @param location used to denote the origin of a given input stream
   * @see #fromInputStream(InputStream, String)
   */
  public static InputStreamResource tryFromInputStream(InputStream inputStream, String location) {
    try {
      return fromInputStream(inputStream, location);
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> "Can not find resource from input stream");
    }
    return null;
  }

  /**
   * Try to return a URL resource according to the given URL string or null if error occurs.
   *
   * @param url used to build URLResource
   * @return a URLResource or null if error occurs
   * @see #fromUrl(String)
   */
  public static URLResource tryFromUrl(String url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find url resource from %s.", url));
    }
    return null;
  }

  /**
   * Try to return a URL resource according to the given URL or null if error occurs.
   *
   * @param url used to build URLResource
   * @return a URLResource or null if error occurs
   * @see #fromUrl(URL)
   */
  public static URLResource tryFromUrl(URL url) {
    try {
      return fromUrl(url);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find url resource from %s.", url));
    }
    return null;
  }

  /**
   * Try to return an input stream resource according to the given HTTP URL and proxy or null if
   * error occurs.
   *
   * @param url the resource URL
   * @param proxy the proxy through which this connection will be made
   * @see #fromUrl(URL, Proxy)
   */
  public static InputStreamResource tryFromUrl(URL url, Proxy proxy) {
    try {
      return fromUrl(url, proxy);
    } catch (IOException e) {
      logger.log(Level.WARNING, e, () -> format("Can not find url resource from %s.", url));
    }
    return null;
  }

  /**
   * Try to resolve and return a resource from the given path or null if error occurs.
   *
   * @param path the resource path
   * @see #resolve(String)
   */
  public static <T extends URLResource> T tryResolve(String path) {
    try (Stream<T> stream = from(path)) {
      return stream.min(Comparator.comparing(URLResource::getURI)).orElse(null);
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> format("Can not find url resource from %s.", path));
    }
    return null;
  }
}

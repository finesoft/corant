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

import static org.corant.shared.normal.Defaults.FOUR_KB;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.isNoneNull;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.Checksum;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;

/**
 * corant-shared
 *
 * Some code from : org.apache.commons.io.FileUtils
 *
 * @author bingo 下午3:54:56
 *
 */
public class FileUtils {

  public static final File[] EMPTY_ARRAY = {};
  public static final char EXTENSION_SEPARATOR = '.';
  public static final String EXTENSION_SEPARATOR_STR = Character.toString(EXTENSION_SEPARATOR);
  public static final char UNIX_SEPARATOR = '/';
  public static final String UNIX_SEPARATOR_STR = Character.toString(UNIX_SEPARATOR);
  public static final char WINDOWS_SEPARATOR = '\\';
  public static final long FILE_COPY_BUFFER_SIZE = Defaults.ONE_MB << 4;
  public static final String JAR_URL_SEPARATOR = "!/";
  public static final String FILE_URL_PREFIX = "file:";
  protected static final Logger logger = Logger.getLogger(FileUtils.class.getName());
  static final String[] JARS = {"jar", "war, ", "zip", "vfszip", "wsjar"};

  private FileUtils() {}

  public static Checksum checksum(final File file, final Checksum checksum) throws IOException {
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Checksums can't be computed on directories");
    }
    try (InputStream is = new FileInputStream(file)) {
      byte[] buffer = new byte[8192];
      int length;
      while ((length = is.read(buffer)) != -1) {
        checksum.update(buffer, 0, length);
      }
    }
    return checksum;
  }

  public static void copyFile(final File srcFile, final File destFile,
      final boolean preserveFileDate) throws IOException {
    copyFile(srcFile, destFile, FILE_COPY_BUFFER_SIZE, preserveFileDate);

  }

  public static void copyFile(final File srcFile, final File destFile, final long bufferSize,
      final boolean preserveFileDate) throws IOException {
    if (destFile.exists() && destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' exists but is a directory");
    }
    try (FileInputStream fis = new FileInputStream(srcFile);
        FileChannel input = fis.getChannel();
        FileOutputStream fos = new FileOutputStream(destFile);
        FileChannel output = fos.getChannel()) {
      final long size = input.size();
      long pos = 0;
      long count;
      while (pos < size && !Thread.currentThread().isInterrupted()) {
        final long remain = size - pos;
        count = remain > bufferSize ? bufferSize : remain;
        final long bytesCopied = output.transferFrom(input, pos, count);
        if (bytesCopied == 0) {
          break;
        }
        pos += bytesCopied;
      }
    }
    final long srcLen = srcFile.length();
    final long dstLen = destFile.length();
    if (srcLen != dstLen) {
      throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile
          + "' Expected length: " + srcLen + " Actual: " + dstLen);
    }
    if (preserveFileDate && destFile.setLastModified(srcFile.lastModified())) {
      logger.warning(() -> String.format("Can not preserve file date for file %s!",
          destFile.getAbsolutePath()));
    }
  }

  public static void copyToFile(final InputStream source, final File destination)
      throws IOException {
    copyToFile(source, destination, FOUR_KB);
  }

  public static void copyToFile(final InputStream source, final File destination, int bufferSize)
      throws IOException {
    try (InputStream in = source; OutputStream out = new FileOutputStream(destination)) {
      Streams.copy(in, out, bufferSize);
    }
  }

  public static File createTempDir(String prefix, String suffix) {
    try {
      File tempDir = File.createTempFile(prefix, suffix);
      boolean notExist = true;
      if (tempDir.exists()) {
        notExist = tempDir.delete();
      }
      if (notExist) {
        if (tempDir.mkdir()) {
          logger.fine(() -> String.format("Created temp dir %s!", tempDir.getAbsolutePath()));
        } else {
          throw new CorantRuntimeException("Unable to create tempDir. java.io.tmpdir is set to %s."
              + Systems.getProperty("java.io.tmpdir"));
        }
      }
      tempDir.deleteOnExit();
      return tempDir;
    } catch (IOException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static void extractJarFile(Path src, Path dest, Predicate<JarEntry> filter)
      throws IOException {
    shouldBeTrue(src != null && dest != null, "Extract jar file the src and dest can not null");
    try (JarFile jar = new JarFile(shouldNotNull(src.toFile()))) {
      Predicate<JarEntry> useFilter = defaultObject(filter, e -> true);
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry each = entries.nextElement();
        if (useFilter.test(each)) {
          Path eachPath = dest.resolve(each.getName().replace('/', File.separatorChar));
          if (each.isDirectory()) {
            if (!java.nio.file.Files.exists(eachPath)) {
              java.nio.file.Files.createDirectories(eachPath);
            }
          } else {
            java.nio.file.Files.copy(jar.getInputStream(each), eachPath);
          }
        }
      }
    }
  }

  public static URL extractJarFileURL(URL jarUrl) throws MalformedURLException {
    if (streamOf(JARS).anyMatch(p -> Objects.areEqual(p, jarUrl.getProtocol()))) {
      String urlFile = jarUrl.getFile();
      int separatorIndex = urlFile.indexOf(JAR_URL_SEPARATOR);
      if (separatorIndex != -1) {
        String jarFile = urlFile.substring(0, separatorIndex);
        try {
          return new URL(jarFile);
        } catch (MalformedURLException ex) {
          if (!jarFile.isEmpty() && jarFile.charAt(0) != UNIX_SEPARATOR) {
            jarFile = UNIX_SEPARATOR + jarFile;
          }
          return new URL(FILE_URL_PREFIX + jarFile);
        }
      } else {
        return jarUrl;
      }
    } else {
      return null;
    }
  }

  public static void forceMkdir(final File directory) throws IOException {
    if (directory.exists()) {
      if (!directory.isDirectory()) {
        final String message = "File " + directory + " exists and is "
            + "not a directory. Unable to create directory.";
        throw new IOException(message);
      }
    } else // Double-check that some other thread or process hasn't made
    // the directory in the background
    if (!directory.mkdirs() && !directory.isDirectory()) {
      final String message = "Unable to create directory " + directory;
      throw new IOException(message);
    }
  }

  /**
   * Returns the file content type corresponding to the path.
   *
   * @param filePath the file path
   * @return getContentType
   */
  public static String getContentType(String filePath) {
    try {
      return java.nio.file.Files.probeContentType(Paths.get(filePath));
    } catch (IOException e) {
      // Noop!
    }
    return null;
  }

  /**
   * Returns the file base name from file, base name is the file name without extension.
   *
   * @param file the file
   * @return getFileBaseName
   */
  public static String getFileBaseName(File file) {
    return getFileBaseName(shouldNotNull(file.getPath()));
  }

  /**
   * Returns the file base name from path, base name is the file name without extension.
   *
   * @param path the path
   * @return getFileBaseName
   */
  public static String getFileBaseName(String path) {
    String fileName = getFileName(path);
    if (fileName != null) {
      String ext = getFileNameExtension(path);
      if (ext == null || areEqual(ext, fileName)) {
        return fileName;
      } else {
        return fileName.substring(0, fileName.length() - ext.length() - 1);
      }
    }
    return null;
  }

  /**
   * Returns the file name from path
   *
   * @param path the file path
   * @return getFileName
   */
  public static String getFileName(String path) {
    if (isBlank(path)) {
      return null;
    } else {
      shouldBeFalse(path.chars().anyMatch(p -> p == 0));
      int idx = max(path.lastIndexOf(UNIX_SEPARATOR), path.lastIndexOf(WINDOWS_SEPARATOR));
      return path.substring(idx + 1);
    }
  }

  /**
   * Returns the file extension from path
   *
   * @param path the file path
   * @return getFileNameExtension
   */
  public static String getFileNameExtension(String path) {
    if (path == null) {
      return null;
    } else {
      int ep = path.lastIndexOf(EXTENSION_SEPARATOR);
      int sp = max(path.lastIndexOf(UNIX_SEPARATOR), path.lastIndexOf(WINDOWS_SEPARATOR));
      if (sp > ep) {
        return null;
      } else {
        return path.substring(ep + 1);
      }
    }
  }

  /**
   * Returns a user defined file attributes map from the given file path if the file system
   * supports.
   *
   * @param path the file path
   * @throws IOException if an I/O error occurs
   *
   * @see UserDefinedFileAttributeView
   */
  public static Map<String, String> getUserDefinedAttributes(final Path path) throws IOException {
    Map<String, String> atts = new LinkedHashMap<>();
    if (Files.getFileStore(shouldNotNull(path))
        .supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
      UserDefinedFileAttributeView view =
          Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
      List<String> attNames = defaultObject(view.list(), Collections::emptyList);
      for (String attName : attNames) {
        int attSize = view.size(attName);
        if (attSize > 0) {
          ByteBuffer buffer = ByteBuffer.allocate(attSize);
          view.read(attName, buffer);
          atts.put(attName, Defaults.DFLT_CHARSET.decode(buffer.flip()).toString());
          buffer.clear();
        }
      }
    } else {
      logger.warning(() -> "The file system can't support user defined file attribute!");
    }
    return atts;
  }

  /**
   * Compare whether two files are the same by byte, if the file is a directory or the file is not
   * readable, it returns False.
   *
   * @param file1 the file to be compared
   * @param file2 the file to be compared
   * @throws IOException if the given file does not exist,is a directory rather than a regular
   *         file,or for some other reason cannot be opened forreading
   */
  public static boolean isSameContent(final File file1, final File file2) throws IOException {
    if (file1 == null || file2 == null || !file1.isFile() || !file2.isFile() || !file1.canRead()
        || !file2.canRead()) {
      return false;
    }
    try (FileInputStream fis1 = new FileInputStream(file1);
        BufferedInputStream bis1 = new BufferedInputStream(fis1);
        FileInputStream fis2 = new FileInputStream(file2);
        BufferedInputStream bis2 = new BufferedInputStream(fis2)) {
      int b1 = 0;
      int b2 = 0;
      while (b1 != -1 && b2 != -1) {
        if (b1 != b2) {
          return false;
        }
        b1 = bis1.read();
        b2 = bis2.read();
      }
      if (b1 != b2) {
        return false;
      }
    }
    return true;
  }

  /**
   * Put the given user defined file attributes map to the given file path if the file system
   * supports.
   *
   * @param path the file path
   * @param attributes the attributes to put
   * @param overwrite whether to overwrite an existing attribute with the same name
   * @throws IOException if an I/O error occurs
   */
  public static void putUserDefinedAttributes(final Path path, Map<String, String> attributes,
      boolean overwrite) throws IOException {
    if (isEmpty(attributes)) {
      return;
    }
    if (Files.getFileStore(shouldNotNull(path))
        .supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
      UserDefinedFileAttributeView view =
          Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
      List<String> exists = defaultObject(view.list(), Collections::emptyList);
      for (Entry<String, String> entry : attributes.entrySet()) {
        if (isNoneNull(entry.getKey(), entry.getValue())
            && (overwrite || !exists.contains(entry.getKey()))) {
          view.write(entry.getKey(), Defaults.DFLT_CHARSET.encode(entry.getValue()));
        }
      }
    } else {
      logger.warning(() -> "The file system can't support user defined file attribute!");
    }
  }

}

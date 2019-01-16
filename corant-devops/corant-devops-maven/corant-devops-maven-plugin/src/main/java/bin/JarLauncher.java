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
package bin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.CRC32;

/**
 * corant-devops-maven
 *
 * @author bingo 上午11:33:04
 *
 */
public class JarLauncher {

  public static final String WORK_DIR = ".corant-works";
  public static final Attributes.Name RUNNER_CLS_ATTR_NME = new Attributes.Name("runner-class");
  public static final String MANIFEST = "META-INF/MANIFEST.MF";
  public static final String MAIN = "main";
  public static final String JAREXT = ".jar";
  public static final int JAREXT_LEN = JAREXT.length();
  private final Path workPath;
  private final List<Path> classpaths = new ArrayList<>();

  private String[] args = new String[0];
  private Manifest manifest;

  JarLauncher(String... args) {
    this.args = Arrays.stream(args).filter(arg -> arg.startsWith("-")).toArray(String[]::new);
    Arrays.stream(args).filter(arg -> arg.startsWith("+")).map(arg -> Paths.get(arg.substring(1)))
        .forEach(classpaths::add);
    workPath = Paths.get(System.getProperty("user.home")).resolve(WORK_DIR);
  }

  public static void main(String... args) throws Exception {
    new JarLauncher(args).launch();
  }

  public void launch() {
    try {
      Files.createDirectories(workPath);
      cleanWorkDir();
      extract();
      Class<?> mainClass =
          getClassLoader().loadClass(manifest.getMainAttributes().getValue(RUNNER_CLS_ATTR_NME));
      log(true, "Find application main class %s, the application is starting...", mainClass);
      getMainMethod(mainClass).invoke(null, new Object[] {args});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void cleanWorkDir() {
    if (Arrays.stream(args).anyMatch(arg -> "-cwd".equalsIgnoreCase(arg))) {
      log(true, "Clearing archives from work space %s ...", workPath);
      File file = workPath.toFile();
      if (file.exists()) {
        for (File archive : file.listFiles()) {
          archive.delete();
        }
      }
    }
  }

  void extract() throws IOException, NoSuchAlgorithmException {
    log(true, "Extracting archives to work space %s ...", workPath);
    URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
    if (location.toExternalForm().endsWith(JAREXT)) {
      try (JarFile jar = new JarFile(new File(location.getPath()))) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry each = entries.nextElement();
          if (each.getName().endsWith(JAREXT)) {
            extract(jar, each);
          } else if (each.getName().equals(MANIFEST)) {
            loadManifest(jar, each);
          }
        }
      }
    }
  }

  void extract(JarFile jar, JarEntry entry) throws IOException, NoSuchAlgorithmException {
    String entryName = entry.getName();
    int slashLoc = entryName.lastIndexOf("/");
    if (slashLoc > 0) {
      entryName = entryName.substring(slashLoc + 1);
    }
    String checksum = getChecksum(jar, entry);
    Path dest = workPath
        .resolve(entryName.substring(0, entryName.length() - JAREXT_LEN) + "-" + checksum + JAREXT);
    if (Files.exists(dest)) {
      classpaths.add(dest);
      return;
    }
    classpaths.add(dest);
    try (InputStream in = jar.getInputStream(entry)) {
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  String getChecksum(JarFile jar, JarEntry each) throws FileNotFoundException, IOException {
    CRC32 crc32 = new CRC32();
    try (InputStream is = jar.getInputStream(each)) {
      byte[] buffer = new byte[8192];
      int length;
      while ((length = is.read(buffer)) != -1) {
        crc32.update(buffer, 0, length);
      }
      return crc32.getValue() + "";
    }
  }

  ClassLoader getClassLoader() {
    return new URLClassLoader(classpaths.stream().map(path -> {
      try {
        return path.toUri().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }).toArray(URL[]::new));
  }

  Method getMainMethod(Class<?> cls) throws NoSuchMethodException {
    Method[] methods = cls.getMethods();
    for (Method method : methods) {
      if (method.getName().equals(MAIN) && Modifier.isPublic(method.getModifiers())
          && Modifier.isStatic(method.getModifiers())) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && params[0] == String[].class) {
          return method;
        }
      }
    }
    throw new NoSuchMethodException("public static void main(String...args)");
  }

  void loadManifest(JarFile jar, JarEntry each) throws IOException {
    try (InputStream in = jar.getInputStream(each)) {
      manifest = new Manifest(in);
    }
  }

  void log(boolean newLine, String msgOrFmt, Object... args) {
    if (args.length == 0) {
      if (newLine) {
        System.out.println(msgOrFmt);
      } else {
        System.out.print(msgOrFmt);
      }
    } else {
      if (newLine) {
        System.out.println(String.format(msgOrFmt, args));
      } else {
        System.out.print(String.format(msgOrFmt, args));
      }
    }
  }
}

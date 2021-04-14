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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  public static final String APP_NME_KEY = "corant.application-name";
  public static final String DFLT_APP_NAME = "corant";
  public static final Attributes.Name RUNNER_CLS_ATTR_NME = new Attributes.Name("Runner-Class");
  public static final String MANIFEST = "META-INF/MANIFEST.MF";
  public static final String MAIN = "main";
  public static final String JAREXT = ".jar";
  public static final int JAREXT_LEN = JAREXT.length();
  private final List<Path> classpaths = new ArrayList<>();
  private String appName;
  private String mainClsName;
  private Path workPath;
  private String[] args;
  private Manifest manifest;

  JarLauncher(String... args) throws IOException {
    this.args = Arrays.stream(args).filter(arg -> !arg.isEmpty() && arg.charAt(0) == '-')
        .toArray(String[]::new);
    Arrays.stream(args).filter(arg -> !arg.isEmpty() && arg.charAt(0) == '+')
        .map(arg -> Paths.get(arg.substring(1))).forEach(classpaths::add);
    initialize();
  }

  public static void main(String... args) throws Exception {
    new JarLauncher(args).launch();
  }

  public void launch() {
    try {
      Files.createDirectories(workPath);
      cleanWorkDir();
      extract();
      Class<?> mainClass = buildClassLoader().loadClass(mainClsName);
      System.setProperty(APP_NME_KEY, appName);
      log(true, "Find main class %s by corant class loader, the %s is starting...", mainClass,
          appName);
      getMainMethod(mainClass).invoke(null, new Object[] {args});
    } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchMethodException
        | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  ClassLoader buildClassLoader() {
    return AccessController
        .doPrivileged((PrivilegedAction<URLClassLoader>) () -> new URLClassLoader(DFLT_APP_NAME,
            classpaths.stream().map(path -> {
              try {
                return path.toUri().toURL();
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            }).toArray(URL[]::new), this.getClass().getClassLoader()));
  }

  void cleanWorkDir() {
    if (Arrays.stream(args).anyMatch("-cwd"::equalsIgnoreCase)) {
      log(true, "Clearing archives from workspace %s ...", workPath);
      File file = workPath.toFile();
      if (file != null && file.exists()) {
        File[] files = file.listFiles();
        if (files != null) {
          for (File archive : files) {
            if (archive != null && !archive.delete()) {
              log(true, "[WARNING] Can not clear archive %s.", archive.getPath());
            }
          }
        }
      }
    }
  }

  void extract() throws IOException, NoSuchAlgorithmException {
    long ts = System.currentTimeMillis();
    log(true, "Extracting archives into workspace %s and use CRC32 for checking ...", workPath);
    Set<Path> newJarPaths = new HashSet<>();
    Set<Path> existedJarPaths = new HashSet<>();
    URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
    if (location.toExternalForm().endsWith(JAREXT)) {
      try (JarFile jar = new JarFile(new File(location.getPath()))) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry each = entries.nextElement();
          if (each.getName().endsWith(JAREXT)) {
            extract(jar, each, newJarPaths, existedJarPaths);
          }
        }
      }
    }
    double takeSecs = (System.currentTimeMillis() - ts) / 1000.00;
    log(true,
        "Finished extraction, %d archives were extracted, including %d new ones and %d existing ones,"
            + " take %.2f seconds.",
        newJarPaths.size() + existedJarPaths.size(), newJarPaths.size(), existedJarPaths.size(),
        takeSecs);
  }

  void extract(JarFile jar, JarEntry entry, Set<Path> newJarPaths, Set<Path> existedJarPaths)
      throws IOException, NoSuchAlgorithmException {
    String entryName = entry.getName();
    int slashLoc = entryName.lastIndexOf('/');
    if (slashLoc > 0) {
      entryName = entryName.substring(slashLoc + 1);
    }
    String checksum = getChecksum(jar, entry);
    Path dest = workPath
        .resolve(entryName.substring(0, entryName.length() - JAREXT_LEN) + "-" + checksum + JAREXT);
    if (Files.exists(dest)) {
      classpaths.add(dest);
      existedJarPaths.add(dest);
      return;
    }
    classpaths.add(dest);
    newJarPaths.add(dest);
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

  void initialize() throws IOException {
    loadManifest();
    if (manifest.getMainAttributes().getValue(Attributes.Name.EXTENSION_NAME) == null) {
      appName = DFLT_APP_NAME;
    } else {
      appName = manifest.getMainAttributes().getValue(Attributes.Name.EXTENSION_NAME);
    }
    workPath = Paths.get(System.getProperty("user.home")).resolve("." + appName + "-works");
    mainClsName = manifest.getMainAttributes().getValue(RUNNER_CLS_ATTR_NME);
  }

  void loadManifest() throws IOException {
    URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
    if (location.toExternalForm().endsWith(JAREXT)) {
      try (JarFile jar = new JarFile(new File(location.getPath()))) {
        manifest = jar.getManifest();
      }
    }
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
        System.out.printf(msgOrFmt + "%n", args);
      } else {
        System.out.printf(msgOrFmt, args);
      }
    }
  }

}

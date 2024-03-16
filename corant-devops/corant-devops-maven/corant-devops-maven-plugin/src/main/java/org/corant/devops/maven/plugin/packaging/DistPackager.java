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
package org.corant.devops.maven.plugin.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.corant.devops.maven.plugin.BuildStageException;
import org.corant.devops.maven.plugin.archive.Archive;
import org.corant.devops.maven.plugin.archive.Archive.Entry;
import org.corant.devops.maven.plugin.archive.ClassPathEntry;
import org.corant.devops.maven.plugin.archive.DefaultArchive;
import org.corant.devops.maven.plugin.archive.FileDirectoryArchive;
import org.corant.devops.maven.plugin.archive.FileEntry;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 下午4:33:08
 */
public class DistPackager implements Packager {

  public static final String JVM_OPT = "jvm.options";
  public static final String RUN_BAT = "run.bat";
  public static final String RUNW_BAT = "runw.bat";
  public static final String LAUNCH_BATS =
      "startup.bat,start.bat,stop.bat,restart.bat,shutdown.bat";
  public static final String RUN_SH = "run.sh";
  public static final String RUNW_SH = "runw.sh";
  public static final String LAUNCH_SHS = "startup.sh,start.sh,stop.sh,restart.sh,shutdown.sh";
  public static final String RUN_APP_NAME_PH = "#APPLICATION_NAME#";
  public static final String RUN_APP_ARGS = "#APPLICATION_ARGUMENTS#";
  public static final String RUN_MAIN_CLASS_PH = "#MAIN_CLASS#";
  public static final String RUN_USED_CONFIG_LOCATION = "#USED_CONFIG_LOCATION#";
  public static final String RUN_USED_CONFIG_PROFILE = "#USED_CONFIG_PROFILE#";
  public static final String RUN_ADD_VM_ARGS = "#ADDITIONAL_VM_ARGUMENTS#";
  public static final String RUN_MODULE_ARGS = "#MODULE_ARGUMENTS#";

  public static final String DIST_NAME_SUF = "-dist";

  private final PackageMojo mojo;
  private final Log log;

  DistPackager(PackageMojo mojo) {
    if (!mojo.isJar()) {
      throw new BuildStageException("Only support jar mojo!");
    }
    this.mojo = mojo;
    log = mojo.getLog();
  }

  @Override
  public PackageMojo getMojo() {
    return mojo;
  }

  @Override
  public void pack() throws Exception {
    log.debug("(corant)----------------------------[pack dist]----------------------------");
    log.debug("(corant) start packaging process...");
    doPack(buildArchive());
  }

  protected void doPack(Archive root) throws IOException, ArchiveException {
    final Path destPath = Objects.requireNonNull(resolvePath());
    log.debug(String.format("(corant) created destination url %s for packaging.",
        destPath.toUri().getPath()));
    final Path parentPath = Objects.requireNonNull(destPath.getParent());
    Files.createDirectories(parentPath);
    log.info(String.format("(corant) building dist archive: %s.", destPath));
    try (FileOutputStream fos = new FileOutputStream(destPath.toFile());
        ArchiveOutputStream<ArchiveEntry> aos = packArchiveOutput(fos)) {
      // handle entries
      if (!root.getEntries(null).isEmpty()) {
        for (Entry entry : root) {
          packArchiveEntry(aos, root, entry);
        }
      }
      // handle child archives
      List<Archive> childrenArchives = new LinkedList<>(root.getChildren());
      while (!childrenArchives.isEmpty()) {
        Archive childArchive = childrenArchives.remove(0);
        if (!childArchive.getEntries(null).isEmpty()) {
          for (Entry childEntry : childArchive) {
            packArchiveEntry(aos, childArchive, childEntry);
          }
        }
        childrenArchives.addAll(childArchive.getChildren());
      }
    }
  }

  Archive buildArchive() throws IOException {
    Archive root = DefaultArchive.root();
    // LICENE README NOTICE
    resolveRootResources().forEach(root::addEntry);
    if (getMojo().isGroupApp()) {
      final String appGroupId = getMojo().getProject().getArtifact().getGroupId();
      List<Entry> libs = new ArrayList<>();
      List<Entry> apps = new ArrayList<>();
      apps.add(FileEntry.of(getMojo().getProject().getArtifact().getFile()));
      getMojo().getProject().getArtifacts().forEach(a -> {
        if (Objects.equals(a.getGroupId(), appGroupId)) {
          apps.add(FileEntry.of(a.getFile()));
        } else {
          libs.add(FileEntry.of(a.getFile()));
        }
      });
      DefaultArchive.of(APP_DIR, root).addEntries(apps);
      DefaultArchive.of(LIB_DIR, root).addEntries(libs);
    } else {
      DefaultArchive.of(LIB_DIR, root).addEntries(getMojo().getProject().getArtifacts().stream()
          .map(Artifact::getFile).map(FileEntry::of).collect(Collectors.toList()));
      DefaultArchive.of(APP_DIR, root)
          .addEntry(FileEntry.of(getMojo().getProject().getArtifact().getFile()));
    }
    DefaultArchive.of(CFG_DIR, root).addEntries(resolveConfigFiles());
    DefaultArchive.of(BIN_DIR, root).addEntries(resolveBinFiles());
    if (StringUtils.isNotBlank(getMojo().getAppendDistResourcePaths())) {
      Map<String, List<File>> appendResources =
          resolveAppendResources(getMojo().getAppendDistResourcePaths());
      appendResources.forEach((name, files) -> {
        Archive archive = DefaultArchive.of(name, root);
        for (File file : files) {
          if (file.isDirectory()) {
            FileDirectoryArchive.of(file, archive);
          } else {
            archive.addEntry(FileEntry.of(file));
          }
        }
      });
    }
    log.debug(
        String.format("(corant) built archive %s for packaging.", root.getEntries(null).size()));
    return root;
  }

  Map<String, List<File>> resolveAppendResources(String resources) {
    Map<String, List<File>> entries = new LinkedHashMap<>();
    String[] nameAndPaths = StringUtils.split(resources.trim(), ";");
    for (String nap : nameAndPaths) {
      boolean found = false;
      if (StringUtils.isNotBlank(nap)) {
        String[] nameAndPath = StringUtils.split(nap.trim(), ",");
        if (nameAndPath.length == 2 && StringUtils.isNotBlank(nameAndPath[0])
            && StringUtils.isNotBlank(nameAndPath[1])) {
          String name = nameAndPath[0].trim();
          String path = nameAndPath[1].trim();
          File file;
          if (!APP_DIR.equals(name) && !LOG_DIR.equals(name) && !LIB_DIR.equals(name)
              && !CFG_DIR.equals(name) && !BIN_DIR.equals(name)
              && (file = new File(path)).exists()) {
            entries.computeIfAbsent(name, k1 -> new ArrayList<>()).add(file);
            log.debug(String.format("(corant) resolve append resource file %s: %s.", name, file));
            found = true;
          }
        }
      }
      if (!found) {
        log.warn(String.format("(corant) append resource %s for packaging is illegal", nap));
      }
    }
    return entries;
  }

  List<Entry> resolveBinFiles() throws IOException {
    List<Entry> entries = new ArrayList<>();
    if (getMojo().isUseJavaw()) {
      entries.add(resolveRunbat(RUNW_BAT));
      entries.add(resolveRunsh(RUNW_SH));
    } else {
      entries.add(resolveRunbat(RUN_BAT));
      entries.add(resolveRunsh(RUN_SH));
    }
    entries.addAll(resolveLaunchs());
    return entries;
  }

  List<Entry> resolveConfigFiles() {
    List<Entry> entries = new ArrayList<>();
    String regex = getMojo().getDistConfigPaths();
    List<Pattern> patterns = GlobPatterns.buildAll(regex, false, true);
    final File artDir = new File(getMojo().getProject().getBuild().getOutputDirectory());
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(artDir);
    scanner.scan();
    for (final String file : scanner.getIncludedFiles()) {
      if (GlobPatterns.matchPath(file, patterns)) {
        log.debug(String.format("(corant) resolve configuration file %s.", file));
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    if (getMojo().isWithJvmOpts()) {
      entries.add(ClassPathEntry.of(JVM_OPT, JVM_OPT));
    }
    return entries;
  }

  List<Entry> resolveLaunchs() throws IOException {
    List<Entry> entries = new ArrayList<>();
    if (getMojo().isUseDirectRunner() && !getMojo().isUseJavaw()) {
      String[] bats = LAUNCH_BATS.split(",");
      String applicationName = resolveApplicationName();
      for (String bat : bats) {
        String runbat = IOUtils.toString(ClassPathEntry.of(bat, bat).getInputStream(), CHARSET);
        final String usebat = runbat.replace(RUN_APP_NAME_PH, applicationName);
        log.debug(String.format("(corant) resolve launch file %s.", bat));
        entries.add(new ScriptEntry(bat, usebat));
      }
    }
    return entries;
  }

  Path resolvePath() {
    Path target = Paths.get(getMojo().getProject().getBuild().getDirectory());
    return target.resolve(getMojo().getFinalName().concat("-").concat(getMojo().getClassifier())
        .concat(DIST_NAME_SUF).concat(".").concat(getMojo().getDistFormat()));
  }

  List<Entry> resolveRootResources() throws IOException {
    List<Entry> entries = new ArrayList<>();
    String regex = getMojo().getDistResourcePaths();
    List<Pattern> patterns = GlobPatterns.buildAll(regex, false, true);
    final File artDir = new File(getMojo().getProject().getBuild().getOutputDirectory());
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(artDir);
    scanner.scan();
    for (final String file : scanner.getIncludedFiles()) {
      if (GlobPatterns.matchPath(file, patterns)) {
        log.debug(String.format("(corant) resolve resource file %s.", file));
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    return entries;
  }

  Entry resolveRunbat(String run_bat) throws IOException {
    String runbat = IOUtils.toString(ClassPathEntry.of(run_bat, run_bat).getInputStream(), CHARSET);
    String applicationName = resolveApplicationName();
    final String usebat = runbat.replace(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replace(RUN_APP_NAME_PH, applicationName)
        .replace(RUN_USED_CONFIG_LOCATION, getMojo().getUsedConfigLocation())
        .replace(RUN_USED_CONFIG_PROFILE, getMojo().getUsedConfigProfile())
        .replace(RUN_APP_ARGS,
            getMojo().isUseDirectRunner()
                ? getMojo().getAppArgs().isEmpty() ? getMojo().getAppArgs().concat("%1")
                    : getMojo().getAppArgs().concat(" %1")
                : getMojo().getAppArgs())
        .replace(RUN_ADD_VM_ARGS, getMojo().getVmArgs())
        .replace(RUN_MODULE_ARGS, getMojo().getMiArgs());
    log.debug(String.format("(corant) resolve run command file %s.", run_bat));
    return new ScriptEntry(run_bat, usebat);
  }

  Entry resolveRunsh(String run_sh) throws IOException {
    String runsh = IOUtils.toString(ClassPathEntry.of(run_sh, run_sh).getInputStream(), CHARSET);
    String applicationName = resolveApplicationName();
    final String usesh = runsh.replace(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replace(RUN_APP_NAME_PH, resolveRunshVar(applicationName))
        .replace(RUN_USED_CONFIG_LOCATION, resolveRunshVar(getMojo().getUsedConfigLocation()))
        .replace(RUN_USED_CONFIG_PROFILE, resolveRunshVar(getMojo().getUsedConfigProfile()))
        .replace(RUN_APP_ARGS, resolveRunshVar(getMojo().getAppArgs()))
        .replace(RUN_ADD_VM_ARGS, resolveRunshVar(getMojo().getVmArgs()))
        .replace(RUN_MODULE_ARGS, getMojo().getMiArgs());
    log.debug(String.format("(corant) resolve run command file %s.", run_sh));
    return new ScriptEntry(run_sh, usesh);
  }

  private void packArchiveEntry(ArchiveOutputStream<ArchiveEntry> aos, Archive archive, Entry entry)
      throws IOException {
    String entryName = resolveArchivePath(archive.getPath(), entry.getName());
    File file;
    if (entry instanceof FileEntry) {
      file = ((FileEntry) entry).getFile();
    } else {
      log.debug(String.format("(corant) create temp file for archive file entry %s.", entryName));
      file = Files.createTempFile("corant-mojo-pack", entry.getName()).toFile();
      try (OutputStream os = new FileOutputStream(file)) {
        IOUtils.copy(entry.getInputStream(), os);
      }
      file.deleteOnExit();
    }
    aos.putArchiveEntry(aos.createArchiveEntry(file, entryName));
    try (InputStream fis = new FileInputStream(file)) {
      IOUtils.copy(fis, aos);
    }
    aos.closeArchiveEntry();
    log.debug(String.format("(corant) entry %s was packaged.", entryName));
  }

  private ArchiveOutputStream<ArchiveEntry> packArchiveOutput(OutputStream os)
      throws ArchiveException {
    return new ArchiveStreamFactory().createArchiveOutputStream(mojo.getDistFormat(), os);
  }

  private String resolveRunshVar(String variable) {
    if (!variable.isEmpty()) {
      return "\"".concat(variable).concat("\"");
    }
    return variable;
  }

  /**
   * corant-devops-maven-plugin
   *
   * @author bingo 下午12:09:52
   */
  static final class ScriptEntry implements Entry {
    private final String script;
    private final String name;

    ScriptEntry(String name, String script) {
      this.name = name;
      this.script = script;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return IOUtils.toInputStream(script, CHARSET);
    }

    @Override
    public String getName() {
      return name;
    }
  }
}

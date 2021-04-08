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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.corant.devops.maven.plugin.BuildStageException;
import org.corant.devops.maven.plugin.archive.Archive;
import org.corant.devops.maven.plugin.archive.Archive.Entry;
import org.corant.devops.maven.plugin.archive.ClassPathEntry;
import org.corant.devops.maven.plugin.archive.DefaultArchive;
import org.corant.devops.maven.plugin.archive.FileEntry;

/**
 * corant-devops-maven
 *
 * @author bingo 下午4:33:08
 *
 */
public class DistPackager implements Packager {

  public static final String JVM_OPT = "jvm.options";
  public static final String RUN_BAT = "run.bat";
  public static final String LAUNCH_BATS =
      "startup.bat,start.bat,stop.bat,restart.bat,shutdown.bat";
  public static final String RUN_SH = "run.sh";
  public static final String LAUNCH_SHS = "startup.sh,start.sh,stop.sh,restart.sh,shutdown.sh";
  public static final String RUN_APP_NAME_PH = "#APPLICATION_NAME#";
  public static final String RUN_APP_ARGS = "#APPLICATION_ARGUMENTS#";
  public static final String RUN_MAIN_CLASS_PH = "#MAIN_CLASS#";
  public static final String RUN_USED_CONFIG_LOCATION = "#USED_CONFIG_LOCATION#";
  public static final String RUN_USED_CONFIG_PROFILE = "#USED_CONFIG_PROFILE#";
  public static final String RUN_ADD_VM_ARGS = "#ADDITIONAL_VM_ARGUMENTS#";

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
        ArchiveOutputStream aos = packArchiveOutput(fos)) {
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
      getMojo().getProject().getArtifacts().stream().forEach(a -> {
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
    log.debug(
        String.format("(corant) built archive %s for packaging.", root.getEntries(null).size()));
    return root;
  }

  List<Entry> resolveBinFiles() throws IOException {
    List<Entry> entries = new ArrayList<>();
    entries.add(resolveRunbat());
    entries.add(resolveRunsh());
    entries.addAll(resolveLaunchs());
    return entries;
  }

  List<Entry> resolveConfigFiles() {
    List<Entry> entries = new ArrayList<>();
    String regex = getMojo().getDistConfigPaths();
    List<Pattern> patterns = Arrays.stream(regex.split(",")).filter(Objects::nonNull)
        .map(p -> GlobPatterns.build(p, false, true)).collect(Collectors.toList());
    final File artDir = new File(getMojo().getProject().getBuild().getOutputDirectory());
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(artDir);
    scanner.scan();
    for (final String file : scanner.getIncludedFiles()) {
      if (patterns.stream().anyMatch(p -> p.matcher(file.replaceAll("\\\\", "/")).matches())) {
        log.debug(String.format("(corant) resolve configuration file %s.", file));
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    entries.add(ClassPathEntry.of(JVM_OPT, JVM_OPT));
    return entries;
  }

  List<Entry> resolveLaunchs() throws IOException {
    List<Entry> entries = new ArrayList<>();
    if (getMojo().isUseDirectRunner()) {
      String[] bats = LAUNCH_BATS.split(",");
      String applicationName = resolveApplicationName();
      for (String bat : bats) {
        String runbat = IOUtils.toString(ClassPathEntry.of(bat, bat).getInputStream(), CHARSET);
        final String usebat = runbat.replaceAll(RUN_APP_NAME_PH, applicationName);
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
    List<Pattern> patterns = Arrays.stream(regex.split(",")).filter(Objects::nonNull)
        .map(p -> GlobPatterns.build(p, false, true)).collect(Collectors.toList());
    final File artDir = new File(getMojo().getProject().getBuild().getOutputDirectory());
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(artDir);
    scanner.scan();
    for (final String file : scanner.getIncludedFiles()) {
      if (patterns.stream().anyMatch(p -> p.matcher(file.replaceAll("\\\\", "/")).matches())) {
        log.debug(String.format("(corant) resolve resource file %s.", file));
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    return entries;
  }

  Entry resolveRunbat() throws IOException {
    String runbat = IOUtils.toString(ClassPathEntry.of(RUN_BAT, RUN_BAT).getInputStream(), CHARSET);
    String applicationName = resolveApplicationName();
    final String usebat = runbat.replaceAll(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replaceAll(RUN_APP_NAME_PH, applicationName)
        .replaceAll(RUN_USED_CONFIG_LOCATION, getMojo().getUsedConfigLocation())
        .replaceAll(RUN_USED_CONFIG_PROFILE, getMojo().getUsedConfigProfile())
        .replaceAll(RUN_APP_ARGS,
            getMojo().isUseDirectRunner()
                ? getMojo().getAppArgs().isEmpty() ? getMojo().getAppArgs().concat("%1")
                    : getMojo().getAppArgs().concat(" %1")
                : getMojo().getAppArgs())
        .replaceAll(RUN_ADD_VM_ARGS, getMojo().getVmArgs());
    log.debug(String.format("(corant) resolve run command file %s.", RUN_BAT));
    return new ScriptEntry(RUN_BAT, usebat);
  }

  Entry resolveRunsh() throws IOException {
    String runsh = IOUtils.toString(ClassPathEntry.of(RUN_SH, RUN_SH).getInputStream(), CHARSET);
    String applicationName = resolveApplicationName();
    final String usesh = runsh.replaceAll(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replaceAll(RUN_APP_NAME_PH, resolveRunshVar(applicationName))
        .replaceAll(RUN_USED_CONFIG_LOCATION, resolveRunshVar(getMojo().getUsedConfigLocation()))
        .replaceAll(RUN_USED_CONFIG_PROFILE, resolveRunshVar(getMojo().getUsedConfigProfile()))
        .replaceAll(RUN_APP_ARGS, resolveRunshVar(getMojo().getAppArgs()))
        .replaceAll(RUN_ADD_VM_ARGS, resolveRunshVar(getMojo().getVmArgs()));
    log.debug(String.format("(corant) resolve run command file %s.", RUN_SH));
    return new ScriptEntry(RUN_SH, usesh);
  }

  private void packArchiveEntry(ArchiveOutputStream aos, Archive archive, Entry entry)
      throws IOException {
    String entryName = resolveArchivePath(archive.getPath(), entry.getName());
    File file;
    if (entry instanceof FileEntry) {
      file = ((FileEntry) entry).getFile();
    } else {
      log.debug(String.format("(corant) create temp entry file for entry %s.", entryName));
      file = Files.createTempFile("corant-mojo-pack", entry.getName()).toFile();
      IOUtils.copy(entry.getInputStream(), new FileOutputStream(file));
      file.deleteOnExit();
    }
    aos.putArchiveEntry(aos.createArchiveEntry(file, entryName));
    IOUtils.copy(new FileInputStream(file), aos);
    aos.closeArchiveEntry();
    log.debug(String.format("(corant) packaged entry %s.", entryName));
  }

  private ArchiveOutputStream packArchiveOutput(OutputStream os) throws ArchiveException {
    return new ArchiveStreamFactory().createArchiveOutputStream(mojo.getDistFormat(), os);
  }

  private String resolveRunshVar(String var) {
    if (!var.isEmpty()) {
      return "\"".concat(var).concat("\"");
    }
    return var;
  }

  /**
   * corant-devops-maven-plugin
   *
   * @author bingo 下午12:09:52
   *
   */
  static final class ScriptEntry implements Entry {
    private final String script;
    private final String name;

    /**
     * @param script
     */
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

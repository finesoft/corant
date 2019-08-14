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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
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
  public static final String RUN_SH = "run.sh";
  public static final String RUN_APP_NAME_PH = "#APPLICATION_NAME#";
  public static final String RUN_MAIN_CLASS_PH = "#MAIN_CLASS#";
  public static final String RUN_USED_CONFIG_LOCATION = "#USED_CONFIG_LOCATION#";
  public static final String DIST_NAME_SUF = "-dist.zip";

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

  protected void doPack(Archive root) throws IOException {
    final Path destPath = Objects.requireNonNull(resolvePath());
    final Path parentPath = Objects.requireNonNull(destPath.getParent());
    Files.createDirectories(parentPath);
    log.debug(String.format("(corant) created destination dir %s for packaging.",
        parentPath.toUri().getPath()));
    try (ZipArchiveOutputStream zos =
        new ZipArchiveOutputStream(new FileOutputStream(destPath.toFile()))) {
      // handle entries
      if (!root.getEntries(null).isEmpty()) {
        for (Entry entry : root) {
          ZipArchiveEntry zipEntry =
              new ZipArchiveEntry(resolveArchivePath(root.getPath(), entry.getName()));
          zos.putArchiveEntry(zipEntry);
          IOUtils.copy(entry.getInputStream(), zos);
          zos.closeArchiveEntry();
          log.debug(String.format("(corant) packaged entry %s", zipEntry.getName()));
        }
      }
      // handle child archives
      List<Archive> childrenArchives = new LinkedList<>(root.getChildren());
      while (!childrenArchives.isEmpty()) {
        Archive childArchive = childrenArchives.remove(0);
        if (!childArchive.getEntries(null).isEmpty()) {
          for (Entry childEntry : childArchive) {
            ZipArchiveEntry childZipEntry = new ZipArchiveEntry(
                resolveArchivePath(childArchive.getPath(), childEntry.getName()));
            zos.putArchiveEntry(childZipEntry);
            IOUtils.copy(childEntry.getInputStream(), zos);
            zos.closeArchiveEntry();
            log.debug(String.format("(corant) packaged entry %s", childZipEntry.getName()));
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
    DefaultArchive.of(LIB_DIR, root).addEntries(getMojo().getProject().getArtifacts().stream()
        .map(Artifact::getFile).map(FileEntry::of).collect(Collectors.toList()));
    DefaultArchive.of(APP_DIR, root)
        .addEntry(FileEntry.of(getMojo().getProject().getArtifact().getFile()));
    DefaultArchive.of(CFG_DIR, root).addEntries(resolveConfigFiles());
    DefaultArchive.of(BIN_DIR, root).addEntries(resolveBinFiles());
    log.debug("(corant) built archive for packaging.");
    return root;
  }

  List<Entry> resolveBinFiles() throws IOException {
    List<Entry> entries = new ArrayList<>();
    entries.add(resolveRunbat());
    entries.add(resolveRunsh());
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
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    entries.add(ClassPathEntry.of(JVM_OPT, JVM_OPT));
    return entries;
  }

  Path resolvePath() {
    Path target = Paths.get(getMojo().getProject().getBuild().getDirectory());
    return target
        .resolve(getMojo().getFinalName() + "-" + getMojo().getClassifier() + DIST_NAME_SUF);
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
        entries.add(FileEntry.of(new File(artDir, file)));
      }
    }
    return entries;
  }

  Entry resolveRunbat() throws IOException {
    String runbat = IOUtils.toString(ClassPathEntry.of(RUN_BAT, RUN_BAT).getInputStream(), CHARSET);
    final String usebat = runbat.replaceAll(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replaceAll(RUN_APP_NAME_PH, resolveApplicationName())
        .replaceAll(RUN_USED_CONFIG_LOCATION, getMojo().getUsedConfigLocation());
    return new ScriptEntry(RUN_BAT, usebat);
  }

  Entry resolveRunsh() throws IOException {
    String runsh = IOUtils.toString(ClassPathEntry.of(RUN_SH, RUN_SH).getInputStream(), CHARSET);
    final String usesh = runsh.replaceAll(RUN_MAIN_CLASS_PH, getMojo().getMainClass())
        .replaceAll(RUN_APP_NAME_PH, resolveApplicationName())
        .replaceAll(RUN_USED_CONFIG_LOCATION, getMojo().getUsedConfigLocation());
    return new ScriptEntry(RUN_SH, usesh);
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
     * @param usebat
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

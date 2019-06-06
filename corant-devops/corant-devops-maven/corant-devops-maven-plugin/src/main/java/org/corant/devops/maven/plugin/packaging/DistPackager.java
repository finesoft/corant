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
import java.nio.charset.StandardCharsets;
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
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
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

  public static final String JAR_LIB_DIR = "lib";
  public static final String JAR_APP_DIR = "app";
  public static final String JAR_CFG_DIR = "cfg";
  public static final String JAR_BIN_DIR = "bin";
  public static final String LOG_BIN_DIR = "log";

  private final PackageMojo mojo;
  final Log log;

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
    log.debug(
        "(corant)--------------------------------[pack dist]--------------------------------");
    log.debug("(corant) start packaging process...");
    doPack(buildArchive());
  }

  protected void doPack(Archive root) throws IOException {
    Path destPath = resolvePath();
    Files.createDirectories(destPath.getParent());
    log.debug(String.format("(corant) created destination dir %s for packaging.",
        destPath.getParent().toUri().getPath()));
    try (ZipArchiveOutputStream jos =
        new ZipArchiveOutputStream(new FileOutputStream(destPath.toFile()))) {
      // handle entries
      if (!root.getEntries(null).isEmpty()) {
        JarArchiveEntry jarDirEntry = new JarArchiveEntry(root.getPathName());
        jos.putArchiveEntry(jarDirEntry);
        jos.closeArchiveEntry();
        log.debug(String.format("(corant) created dir %s", jarDirEntry.getName()));
        for (Entry entry : root) {
          JarArchiveEntry jarFileEntry = new JarArchiveEntry(root.getPathName() + entry.getName());
          jos.putArchiveEntry(jarFileEntry);
          IOUtils.copy(entry.getInputStream(), jos);
          jos.closeArchiveEntry();
          log.debug(String.format("(corant) created entry %s", jarFileEntry.getName()));
        }
      }
      // handle child archives
      List<Archive> childrenArchives = new LinkedList<>(root.getChildren());
      while (!childrenArchives.isEmpty()) {
        Archive childArchive = childrenArchives.remove(0);
        if (!childArchive.getEntries(null).isEmpty()) {
          JarArchiveEntry childJarDirEntry = new JarArchiveEntry(childArchive.getPathName());
          jos.putArchiveEntry(childJarDirEntry);
          jos.closeArchiveEntry();
          log.debug(String.format("(corant) created dir %s", childJarDirEntry.getName()));
          for (Entry childEntry : childArchive) {
            JarArchiveEntry childJarFileEntry =
                new JarArchiveEntry(childArchive.getPathName() + childEntry.getName());
            jos.putArchiveEntry(childJarFileEntry);
            IOUtils.copy(childEntry.getInputStream(), jos);
            jos.closeArchiveEntry();
            log.debug(String.format("(corant) created entry %s", childJarFileEntry.getName()));
          }
        }
        childrenArchives.addAll(childArchive.getChildren());
      }
    }
  }

  Archive buildArchive() throws IOException {
    Archive root = DefaultArchive.root();
    DefaultArchive.of(JAR_LIB_DIR, root).addEntries(getMojo().getProject().getArtifacts().stream()
        .map(Artifact::getFile).map(FileEntry::of).collect(Collectors.toList()));
    DefaultArchive.of(JAR_APP_DIR, root)
        .addEntry(FileEntry.of(getMojo().getProject().getArtifact().getFile()));
    DefaultArchive.of(JAR_CFG_DIR, root).addEntries(resolveCfgFiles());
    DefaultArchive.of(JAR_BIN_DIR, root).addEntries(resolveBinFiles());
    log.debug("(corant) built archive for packaging.");
    return root;
  }

  List<Entry> resolveBinFiles() throws IOException {
    List<Entry> entries = new ArrayList<>();
    entries.add(resolveRunbat());
    return entries;
  }

  List<Entry> resolveCfgFiles() {
    List<Entry> entries = new ArrayList<>();
    String regex = getMojo().getConfigPaths();
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
    entries.add(ClassPathEntry.of("jvm.options", "jvm.options"));
    return entries;
  }

  Path resolvePath() {
    Path target = Paths.get(getMojo().getProject().getBuild().getDirectory());
    return target.resolve(getMojo().getFinalName() + "-" + getMojo().getClassifier() + "-dist.zip");
  }

  Entry resolveRunbat() throws IOException {
    String runbat = IOUtils.toString(ClassPathEntry.of("run.bat", "run.bat").getInputStream(),
        StandardCharsets.UTF_8);
    final String useRunbat = runbat.replaceAll("#MAIN_CLASS#", getMojo().getMainClass())
        .replaceAll("#TITLE#", resolveApplicationName());
    return new Entry() {
      @Override
      public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(useRunbat, StandardCharsets.UTF_8);
      }

      @Override
      public String getName() {
        return "run.bat";
      }
    };
  }
}

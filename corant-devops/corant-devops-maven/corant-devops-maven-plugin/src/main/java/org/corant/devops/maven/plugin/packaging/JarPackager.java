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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.corant.devops.maven.plugin.BuildStageException;
import org.corant.devops.maven.plugin.archive.Archive;
import org.corant.devops.maven.plugin.archive.Archive.Entry;
import org.corant.devops.maven.plugin.archive.ClassPathEntry;
import org.corant.devops.maven.plugin.archive.DefaultArchive;
import org.corant.devops.maven.plugin.archive.FileEntry;
import org.corant.devops.maven.plugin.archive.ManifestEntry;
import bin.JarLauncher;

/**
 * corant-devops-maven
 *
 * @author bingo 下午4:33:08
 *
 */
public class JarPackager implements Packager {

  public static final String JAR_LAU_PATH =
      JarLauncher.class.getName().replaceAll("\\.", "/") + ".class";
  public static final String JAR_LAU_NME = JarLauncher.class.getSimpleName() + ".class";

  private final PackageMojo mojo;
  private final Log log;

  JarPackager(PackageMojo mojo) {
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
    log.debug("(corant)----------------------------[pack jar]----------------------------");
    log.debug("(corant) start packaging process...");
    doPack(buildArchive());
  }

  protected void doPack(Archive root) throws IOException {
    final Path destPath = Objects.requireNonNull(resolvePath());
    log.debug(String.format("(corant) created destination dir %s for packaging.",
        destPath.toUri().getPath()));
    final Path parentPath = Objects.requireNonNull(destPath.getParent());
    Files.createDirectories(parentPath);
    log.info(String.format("(corant) building jar: %s", destPath));
    try (JarArchiveOutputStream jos =
        new JarArchiveOutputStream(new FileOutputStream(destPath.toFile()))) {
      // handle entries
      if (!root.getEntries(null).isEmpty()) {
        for (Entry entry : root) {
          JarArchiveEntry jarFileEntry =
              new JarArchiveEntry(resolveArchivePath(root.getPath(), entry.getName()));
          jos.putArchiveEntry(jarFileEntry);
          IOUtils.copy(entry.getInputStream(), jos);
          jos.closeArchiveEntry();
          log.debug(String.format("(corant) packaged entry %s", jarFileEntry.getName()));
        }
      }
      // handle child archives
      List<Archive> childrenArchives = new LinkedList<>(root.getChildren());
      while (!childrenArchives.isEmpty()) {
        Archive childArchive = childrenArchives.remove(0);
        if (!childArchive.getEntries(null).isEmpty()) {
          for (Entry childEntry : childArchive) {
            JarArchiveEntry childJarFileEntry = new JarArchiveEntry(
                resolveArchivePath(childArchive.getPath(), childEntry.getName()));
            jos.putArchiveEntry(childJarFileEntry);
            IOUtils.copy(childEntry.getInputStream(), jos);
            jos.closeArchiveEntry();
            log.debug(String.format("(corant) packaged entry %s", childJarFileEntry.getName()));
          }
        }
        childrenArchives.addAll(childArchive.getChildren());
      }
    }
  }

  Archive buildArchive() {
    Archive root = DefaultArchive.root();
    DefaultArchive.of(LIB_DIR, root).addEntries(getMojo().getProject().getArtifacts().stream()
        .map(Artifact::getFile).map(FileEntry::of).collect(Collectors.toList()));
    DefaultArchive.of(APP_DIR, root)
        .addEntry(FileEntry.of(getMojo().getProject().getArtifact().getFile()));
    DefaultArchive.of(BIN_DIR, root).addEntry(ClassPathEntry.of(JAR_LAU_PATH, JAR_LAU_NME));
    DefaultArchive.of(META_INF_DIR, root).addEntry(ManifestEntry.of(attr -> {
      // The application main class and runner class
      attr.put(Attributes.Name.MAIN_CLASS, JarLauncher.class.getName());
      attr.put(JarLauncher.RUNNER_CLS_ATTR_NME, getMojo().getMainClass());
      attr.put(Attributes.Name.EXTENSION_NAME, resolveApplicationName());
      attr.put(Attributes.Name.SPECIFICATION_TITLE, getMojo().getProject().getName());
      attr.put(Attributes.Name.SPECIFICATION_VERSION, getMojo().getProject().getVersion());
      attr.put(Attributes.Name.IMPLEMENTATION_TITLE, getMojo().getProject().getName());
      attr.put(Attributes.Name.IMPLEMENTATION_VERSION, getMojo().getProject().getVersion());
      resolveFrameworkVersion().ifPresent(v -> attr.put(FW_VER_KEY, v));
      if (getMojo().getProject().getOrganization() != null) {
        attr.put(Attributes.Name.IMPLEMENTATION_VENDOR,
            getMojo().getProject().getOrganization().getName());
        attr.put(Attributes.Name.SPECIFICATION_VENDOR,
            getMojo().getProject().getOrganization().getName());
      }
    }));

    log.debug("(corant) built archive for packaging.");
    return root;
  }

  Path resolvePath() {
    Path target = Paths.get(getMojo().getProject().getBuild().getDirectory());
    return target.resolve(getMojo().getFinalName() + "-" + getMojo().getClassifier() + ".jar");
  }
}

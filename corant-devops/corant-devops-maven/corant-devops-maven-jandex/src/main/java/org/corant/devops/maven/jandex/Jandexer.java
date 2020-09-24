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
package org.corant.devops.maven.jandex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;

/**
 * corant-devops-maven-jandex
 *
 * @author bingo 下午3:55:35
 *
 */
@Mojo(name = "jandex", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class Jandexer extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().debug("(corant)--------------------------[index classes]--------------------------");
    if (!project.getPackaging().equals("jar") && !project.getPackaging().equals("war")) {
      getLog().debug("(corant) skip jandex index, only support jar or war");
      return;
    }
    final File clsDir = new File(project.getBuild().getOutputDirectory());
    if (!clsDir.exists()) {
      getLog().warn("(corant) can not find file dir, the directory is " + clsDir.getPath());
      return;
    }
    final Indexer indexer = new Indexer();
    final DirectoryScanner scanner = new DirectoryScanner();
    getLog().debug(
        "(corant) start index classes files with jandex, the directory is " + clsDir.getPath());
    scanner.setBasedir(clsDir);
    scanner.scan();
    for (final String file : scanner.getIncludedFiles()) {
      if (file.endsWith(".class")) {
        try (FileInputStream fis = new FileInputStream(new File(clsDir, file))) {
          getLog().debug("(corant) indexing file " + file);
          indexer.index(fis);
        } catch (IOException e) {
          getLog().error(e);
        }
      }
    }
    File idxFile = new File(clsDir, "META-INF/jandex.idx");
    idxFile.getParentFile().mkdirs();
    getLog().info("(corant) building index file: " + idxFile.getPath() + " with jandex.");
    try (FileOutputStream indexOut = new FileOutputStream(idxFile)) {
      IndexWriter writer = new IndexWriter(indexOut);
      writer.write(indexer.complete());
    } catch (IOException e) {
      getLog().warn(e);
    }
  }

}

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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.logging.Log;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 下午1:11:45
 */
public class AttachPackager implements Packager {

  private final PackageMojo mojo;
  private final Log log;

  /**
   * @param mojo
   */
  public AttachPackager(PackageMojo mojo) {
    this.mojo = mojo;
    log = mojo.getLog();
  }

  @Override
  public PackageMojo getMojo() {
    return mojo;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void pack() throws Exception {
    log.debug("(corant)----------------------------[pack attach]----------------------------");
    final Path destPath = Objects.requireNonNull(resolvePath());
    log.debug(String.format("(corant) created destination url %s for packaging.",
        destPath.toUri().getPath()));
    DefaultArtifact artifact = new DefaultArtifact(getMojo().getProject().getGroupId(),
        getMojo().getProject().getArtifactId(), getMojo().getProject().getVersion(),
        getMojo().getProject().getArtifact().getScope(), "jar", null,
        new DefaultArtifactHandler("jar"));
    artifact.setFile(destPath.toFile());
    getMojo().getProject().addAttachedArtifact(artifact);
    log.debug("(corant) packaged attach!");
  }

  Path resolvePath() {
    Path target = Paths.get(getMojo().getProject().getBuild().getDirectory());
    return target.resolve(getMojo().getFinalName() + ".jar");
  }

}

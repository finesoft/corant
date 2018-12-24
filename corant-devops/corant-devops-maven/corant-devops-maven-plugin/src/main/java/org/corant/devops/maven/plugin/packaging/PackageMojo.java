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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * corant-devops-maven
 *
 * @author bingo 下午2:36:26
 *
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PackageMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Parameter(defaultValue = "bin", property = "corant.classifier")
  protected String classifier;

  @Parameter(defaultValue = "${project.build.finalName}", property = "corant.finalName")
  protected String finalName;

  @Parameter(defaultValue = "true", property = "corant.attach")
  protected boolean attach;

  @Parameter(defaultValue = "true", property = "corant.jandex")
  protected boolean jandex;

  @Parameter(property = "corant.mainClass")
  protected String mainClass;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isJar()) {
      try {
        new JarPackager(this).pack();
      } catch (Exception e) {
        throw new MojoExecutionException("Packaging error!", e);
      }
    } else if (isWar()) {
      // TODO
    } else {
      getLog()
          .info("(corant) skipping " + project.getArtifactId() + " as packaging is not jar/war");
    }
  }

  public String getClassifier() {
    return classifier;
  }

  public Path getDestination() {
    Path target = Paths.get(project.getBuild().getDirectory());
    if (isWar()) {
      return target.resolve(finalName + "-" + classifier + ".war");
    }
    return target.resolve(finalName + "-" + classifier + ".jar");
  }

  public String getFinalName() {
    return finalName;
  }

  public String getMainClass() {
    return mainClass;
  }

  public MavenProject getProject() {
    return project;
  }

  public boolean isAttach() {
    return attach;
  }

  public boolean isJar() {
    return project.getPackaging().equals("jar");
  }

  public boolean isWar() {
    return project.getPackaging().equals("war");
  }

}

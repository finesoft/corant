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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

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

  @Component
  MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Parameter(defaultValue = "bin", property = "corant.maven-mojo.classifier")
  protected String classifier;

  @Parameter(defaultValue = "${project.build.finalName}", property = "corant.maven-mojo.final-name")
  protected String finalName;

  @Parameter(defaultValue = "true", property = "corant.maven-mojo.group-app")
  protected boolean groupApp;

  @Parameter(defaultValue = "false", property = "corant.maven-mojo.with-attach")
  protected boolean withAttach;

  @Parameter(defaultValue = "true", property = "corant.maven-mojo.with-uber")
  protected boolean withUber;

  @Parameter(defaultValue = "false", property = "corant.maven-mojo.with-dist")
  protected boolean withDist;

  @Parameter(defaultValue = "zip", property = "corant.maven-mojo.dist-format")
  protected String distFormat;

  @Parameter(defaultValue = "false", property = "corant.maven-mojo.use-direct-runner")
  protected boolean useDirectRunner;

  @Parameter(property = "corant.maven-mojo.main-class")
  protected String mainClass;

  @Parameter
  protected String vmArgs;

  @Parameter
  protected String appArgs;

  @Parameter(defaultValue = "**META-INF/*application*.*,**META-INF/*config*.*,log4j.*,log4j2.*",
      property = "corant.maven-mojo.dist-config-paths")
  protected String distConfigPaths;

  @Parameter(defaultValue = "**README*,**LICENSE*,**NOTICE*",
      property = "corant.maven-mojo.dist-resource-paths")
  protected String distResourcePaths;

  @Parameter(defaultValue = "", property = "corant.maven-mojo.used-config-location")
  protected String usedConfigLocation;

  @Parameter(defaultValue = "", property = "corant.maven-mojo.used-config-profile")
  protected String usedConfigProfile;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isJar()) {
      try {
        if (isWithUber()) {
          new JarPackager(this).pack();
        }
        if (isWithAttach()) {
          new AttachPackager(this).pack();
        }
        if (isWithDist()) {
          new DistPackager(this).pack();
        }
      } catch (Exception e) {
        throw new MojoExecutionException("Packaging error!", e);
      }
    } else if (isWar()) {
      throw new MojoExecutionException("We do not currently support packaging as war!");
    } else {
      getLog()
          .info("(corant) skipping " + project.getArtifactId() + " as packaging is not jar/war");
    }
  }

  public String getAppArgs() {
    return appArgs != null && !appArgs.isEmpty() ? appArgs.trim() : "";
  }

  public String getClassifier() {
    return classifier;
  }

  public String getDistConfigPaths() {
    return distConfigPaths;
  }

  public String getDistFormat() {
    String df = distFormat != null && !distFormat.isEmpty() ? distFormat.trim() : "";
    if (!df.isEmpty() && df.charAt(0) == '.') {
      return df.substring(1);
    }
    return df.isEmpty() ? "zip" : df;
  }

  public String getDistResourcePaths() {
    return distResourcePaths;
  }

  public String getFinalName() {
    return finalName;
  }

  public String getMainClass() {
    return mainClass == null ? "" : mainClass;
  }

  public MavenProject getProject() {
    return project;
  }

  public String getUsedConfigLocation() {
    return usedConfigLocation == null ? "" : usedConfigLocation;
  }

  public String getVmArgs() {
    return vmArgs != null && !vmArgs.isEmpty() ? vmArgs.trim().replace('\n', ' ') : "";
  }

  public boolean isGroupApp() {
    return groupApp;
  }

  public boolean isJar() {
    return project.getPackaging().equals("jar");
  }

  public boolean isUseDirectRunner() {
    return useDirectRunner;
  }

  public boolean isWar() {
    return project.getPackaging().equals("war");
  }

  public boolean isWithAttach() {
    return withAttach;
  }

  public boolean isWithDist() {
    return withDist;
  }

  public boolean isWithUber() {
    return withUber;
  }

  protected String getUsedConfigProfile() {
    return usedConfigProfile == null ? "" : usedConfigProfile;
  }

}

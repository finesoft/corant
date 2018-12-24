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
package org.corant.suites.servlet.metadata;

import javax.servlet.Servlet;
import javax.servlet.annotation.ServletSecurity;

/**
 * corant-suites-servlet
 *
 * @author bingo 上午10:58:19
 *
 */
public class ServletSecurityMetaData {

  private HttpConstraintMetaData httpConstraint;
  private HttpMethodConstraintMetaData[] httpMethodConstraints;
  private Class<? extends Servlet> clazz;

  /**
   * @param httpConstraint
   * @param httpMethodConstraints
   */
  public ServletSecurityMetaData(HttpConstraintMetaData httpConstraint,
      HttpMethodConstraintMetaData[] httpMethodConstraints, Class<? extends Servlet> clazz) {
    super();
    this.httpConstraint = httpConstraint;
    this.httpMethodConstraints = httpMethodConstraints;
    this.clazz = clazz;
  }

  public ServletSecurityMetaData(ServletSecurity anno, Class<? extends Servlet> clazz) {
    if (anno != null) {
      httpConstraint = new HttpConstraintMetaData(anno.value());
      httpMethodConstraints = HttpMethodConstraintMetaData.of(anno.httpMethodConstraints());
      this.clazz = clazz;
    }
  }

  protected ServletSecurityMetaData() {}

  /**
   *
   * @return the clazz
   */
  public Class<? extends Servlet> getClazz() {
    return clazz;
  }

  /**
   *
   * @return the httpConstraint
   */
  public HttpConstraintMetaData getHttpConstraint() {
    return httpConstraint;
  }

  /**
   *
   * @return the httpMethodConstraints
   */
  public HttpMethodConstraintMetaData[] getHttpMethodConstraints() {
    return httpMethodConstraints;
  }

  /**
   *
   * @param httpConstraint the httpConstraint to set
   */
  protected void setHttpConstraint(HttpConstraintMetaData httpConstraint) {
    this.httpConstraint = httpConstraint;
  }

  /**
   *
   * @param httpMethodConstraints the httpMethodConstraints to set
   */
  protected void setHttpMethodConstraints(HttpMethodConstraintMetaData[] httpMethodConstraints) {
    this.httpMethodConstraints = httpMethodConstraints;
  }

}

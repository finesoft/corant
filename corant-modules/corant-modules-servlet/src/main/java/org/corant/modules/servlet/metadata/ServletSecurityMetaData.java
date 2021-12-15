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
package org.corant.modules.servlet.metadata;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Arrays;
import javax.servlet.Servlet;
import javax.servlet.annotation.ServletSecurity;

/**
 * corant-modules-servlet
 *
 * @author bingo 上午10:58:19
 *
 */
public class ServletSecurityMetaData {

  private HttpConstraintMetaData httpConstraint;
  private HttpMethodConstraintMetaData[] httpMethodConstraints = {};
  private Class<? extends Servlet> clazz;

  /**
   * @param httpConstraint
   * @param httpMethodConstraints
   */
  public ServletSecurityMetaData(HttpConstraintMetaData httpConstraint,
      HttpMethodConstraintMetaData[] httpMethodConstraints, Class<? extends Servlet> clazz) {
    setHttpConstraint(httpConstraint);
    setHttpMethodConstraints(httpMethodConstraints);
    setClazz(clazz);
  }

  public ServletSecurityMetaData(ServletSecurity anno, Class<? extends Servlet> clazz) {
    this(new HttpConstraintMetaData(shouldNotNull(anno).value()),
        HttpMethodConstraintMetaData.of(anno.httpMethodConstraints()), clazz);
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
    return Arrays.copyOf(httpMethodConstraints, httpMethodConstraints.length);
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
    this.httpMethodConstraints =
        defaultObject(httpMethodConstraints, new HttpMethodConstraintMetaData[0]);
  }

  /**
   *
   * @param clazz the clazz to set
   */
  private void setClazz(Class<? extends Servlet> clazz) {
    this.clazz = shouldNotNull(clazz);
  }

}

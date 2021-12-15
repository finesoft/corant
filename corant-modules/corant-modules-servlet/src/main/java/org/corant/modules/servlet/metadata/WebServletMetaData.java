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
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import org.corant.shared.util.Strings;

/**
 * corant-modules-servlet
 *
 * @author bingo 上午10:10:29
 *
 */
public class WebServletMetaData {

  private String name;
  private String[] value = Strings.EMPTY_ARRAY;
  private String[] urlPatterns = Strings.EMPTY_ARRAY;
  private int loadOnStartup = -1;
  private WebInitParamMetaData[] initParams = {};
  private boolean asyncSupported;
  private String smallIcon;
  private String largeIcon;
  private String description;
  private String displayName;
  private Class<? extends Servlet> clazz;
  private ServletSecurityMetaData security;
  private MultipartConfigMetaData multipartConfig;

  /**
   * @param name
   * @param value
   * @param urlPatterns
   * @param loadOnStartup
   * @param initParams
   * @param asyncSupported
   * @param smallIcon
   * @param largeIcon
   * @param description
   * @param displayName
   * @param clazz
   * @param security
   * @param multipartConfig
   */
  public WebServletMetaData(String name, String[] value, String[] urlPatterns, int loadOnStartup,
      WebInitParamMetaData[] initParams, boolean asyncSupported, String smallIcon, String largeIcon,
      String description, String displayName, Class<? extends Servlet> clazz,
      ServletSecurityMetaData security, MultipartConfigMetaData multipartConfig) {
    setName(name);
    setValue(value);
    setUrlPatterns(urlPatterns);
    setLoadOnStartup(loadOnStartup);
    setInitParams(initParams);
    setAsyncSupported(asyncSupported);
    setSmallIcon(smallIcon);
    setLargeIcon(largeIcon);
    setDescription(description);
    setDisplayName(displayName);
    setClazz(clazz);
    setSecurity(security);
    setMultipartConfig(multipartConfig);
  }

  public WebServletMetaData(WebServlet anno, ServletSecurity secAnno,
      MultipartConfig multipartConfig, Class<? extends Servlet> clazz) {
    this(shouldNotNull(anno).name(), anno.value(), anno.urlPatterns(), anno.loadOnStartup(),
        WebInitParamMetaData.of(anno.initParams()), anno.asyncSupported(), anno.smallIcon(),
        anno.largeIcon(), anno.description(), anno.displayName(), clazz,
        secAnno == null ? null : new ServletSecurityMetaData(secAnno, clazz),
        multipartConfig == null ? null : new MultipartConfigMetaData(multipartConfig));
  }

  protected WebServletMetaData() {}

  /**
   *
   * @return the clazz
   */
  public Class<? extends Servlet> getClazz() {
    return clazz;
  }

  /**
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   *
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   *
   * @return the initParams
   */
  public WebInitParamMetaData[] getInitParams() {
    return Arrays.copyOf(initParams, initParams.length);
  }

  public Map<String, String> getInitParamsAsMap() {
    Map<String, String> map = new HashMap<>(getInitParams().length);
    for (WebInitParamMetaData ipm : getInitParams()) {
      map.put(ipm.getName(), ipm.getValue());
    }
    return map;
  }

  /**
   *
   * @return the largeIcon
   */
  public String getLargeIcon() {
    return largeIcon;
  }

  /**
   *
   * @return the loadOnStartup
   */
  public int getLoadOnStartup() {
    return loadOnStartup;
  }

  public MultipartConfigMetaData getMultipartConfig() {
    return multipartConfig;
  }

  /**
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   *
   * @return the security
   */
  public ServletSecurityMetaData getSecurity() {
    return security;
  }

  /**
   *
   * @return the smallIcon
   */
  public String getSmallIcon() {
    return smallIcon;
  }

  /**
   *
   * @return the urlPatterns
   */
  public String[] getUrlPatterns() {
    return Arrays.copyOf(urlPatterns, urlPatterns.length);
  }

  /**
   *
   * @return the value
   */
  public String[] getValue() {
    return Arrays.copyOf(value, value.length);
  }

  /**
   *
   * @return the asyncSupported
   */
  public boolean isAsyncSupported() {
    return asyncSupported;
  }

  /**
   *
   * @param asyncSupported the asyncSupported to set
   */
  protected void setAsyncSupported(boolean asyncSupported) {
    this.asyncSupported = asyncSupported;
  }

  /**
   *
   * @param clazz the clazz to set
   */
  protected void setClazz(Class<? extends Servlet> clazz) {
    this.clazz = shouldNotNull(clazz);
  }

  /**
   *
   * @param description the description to set
   */
  protected void setDescription(String description) {
    this.description = description;
  }

  /**
   *
   * @param displayName the displayName to set
   */
  protected void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   *
   * @param initParams the initParams to set
   */
  protected void setInitParams(WebInitParamMetaData[] initParams) {
    this.initParams = defaultObject(initParams, new WebInitParamMetaData[0]);
  }

  /**
   *
   * @param largeIcon the largeIcon to set
   */
  protected void setLargeIcon(String largeIcon) {
    this.largeIcon = largeIcon;
  }

  /**
   *
   * @param loadOnStartup the loadOnStartup to set
   */
  protected void setLoadOnStartup(int loadOnStartup) {
    this.loadOnStartup = loadOnStartup;
  }

  protected void setMultipartConfig(MultipartConfigMetaData multipartConfig) {
    this.multipartConfig = multipartConfig;
  }

  /**
   *
   * @param name the name to set
   */
  protected void setName(String name) {
    this.name = name;
  }

  /**
   *
   * @param security the security to set
   */
  protected void setSecurity(ServletSecurityMetaData security) {
    this.security = security;
  }

  /**
   *
   * @param smallIcon the smallIcon to set
   */
  protected void setSmallIcon(String smallIcon) {
    this.smallIcon = smallIcon;
  }

  /**
   *
   * @param urlPatterns the urlPatterns to set
   */
  protected void setUrlPatterns(String[] urlPatterns) {
    this.urlPatterns = defaultObject(urlPatterns, () -> Strings.EMPTY_ARRAY);
  }

  /**
   *
   * @param value the value to set
   */
  protected void setValue(String[] value) {
    this.value = defaultObject(value, () -> Strings.EMPTY_ARRAY);
  }

}

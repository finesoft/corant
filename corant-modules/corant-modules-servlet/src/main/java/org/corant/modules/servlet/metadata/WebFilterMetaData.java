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
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;
import org.corant.shared.util.Strings;

/**
 * corant-modules-servlet
 *
 * @author bingo 上午10:31:24
 *
 */
public class WebFilterMetaData {

  private String displayName;
  private WebInitParamMetaData[] initParams = {};
  private String filterName;
  private String smallIcon;
  private String largeIcon;
  private String[] servletNames = Strings.EMPTY_ARRAY;
  private String[] value = Strings.EMPTY_ARRAY;
  private String[] urlPatterns = Strings.EMPTY_ARRAY;
  private DispatcherType[] dispatcherTypes = {DispatcherType.REQUEST};
  private boolean asyncSupported;
  private String description;
  private Class<? extends Filter> clazz;

  /**
   * @param displayName
   * @param initParams
   * @param filterName
   * @param smallIcon
   * @param largeIcon
   * @param servletNames
   * @param value
   * @param urlPatterns
   * @param dispatcherTypes
   * @param asyncSupported
   * @param description
   * @param clazz
   */
  public WebFilterMetaData(String displayName, WebInitParamMetaData[] initParams, String filterName,
      String smallIcon, String largeIcon, String[] servletNames, String[] value,
      String[] urlPatterns, DispatcherType[] dispatcherTypes, boolean asyncSupported,
      String description, Class<? extends Filter> clazz) {
    setDisplayName(displayName);
    setInitParams(initParams);
    setFilterName(filterName);
    setSmallIcon(smallIcon);
    setLargeIcon(largeIcon);
    setServletNames(servletNames);
    setValue(value);
    setUrlPatterns(urlPatterns);
    setDispatcherTypes(dispatcherTypes);
    setAsyncSupported(asyncSupported);
    setDescription(description);
    setClazz(clazz);
  }

  public WebFilterMetaData(WebFilter anno, Class<? extends Filter> clazz) {
    this(shouldNotNull(anno).displayName(), WebInitParamMetaData.of(anno.initParams()),
        anno.filterName(), anno.smallIcon(), anno.largeIcon(), anno.servletNames(), anno.value(),
        anno.urlPatterns(), anno.dispatcherTypes(), anno.asyncSupported(), anno.description(),
        clazz);
  }

  protected WebFilterMetaData() {}

  /**
   *
   * @return the clazz
   */
  public Class<? extends Filter> getClazz() {
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
   * @return the dispatcherTypes
   */
  public DispatcherType[] getDispatcherTypes() {
    return Arrays.copyOf(dispatcherTypes, dispatcherTypes.length);
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
   * @return the filterName
   */
  public String getFilterName() {
    return filterName;
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
   * @return the servletNames
   */
  public String[] getServletNames() {
    return Arrays.copyOf(servletNames, servletNames.length);
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
  protected void setClazz(Class<? extends Filter> clazz) {
    this.clazz = shouldNotNull(clazz, "The filter class can not null");
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
   * @param dispatcherTypes the dispatcherTypes to set
   */
  protected void setDispatcherTypes(DispatcherType[] dispatcherTypes) {
    this.dispatcherTypes = dispatcherTypes;
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
   * @param filterName the filterName to set
   */
  protected void setFilterName(String filterName) {
    this.filterName = filterName;
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
   * @param servletNames the servletNames to set
   */
  protected void setServletNames(String[] servletNames) {
    this.servletNames = defaultObject(servletNames, () -> Strings.EMPTY_ARRAY);
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

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

import static org.corant.shared.util.Preconditions.requireNotNull;
import java.util.Collection;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

/**
 * corant-suites-servlet
 *
 * @author bingo 上午10:31:24
 *
 */
public class WebFilterMetaData {

  private String displayName;
  private WebInitParamMetaData[] initParams = new WebInitParamMetaData[0];
  private String filterName;
  private String smallIcon;
  private String largeIcon;
  private String[] servletNames = new String[0];
  private String[] value = new String[0];
  private String[] urlPatterns = new String[0];
  private DispatcherType[] dispatcherTypes = new DispatcherType[] {DispatcherType.REQUEST};
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
  public WebFilterMetaData(String displayName, Collection<WebInitParamMetaData> initParams,
      String filterName, String smallIcon, String largeIcon, Collection<String> servletNames,
      Collection<String> value, Collection<String> urlPatterns,
      Collection<DispatcherType> dispatcherTypes, boolean asyncSupported, String description,
      Class<? extends Filter> clazz) {
    super();
    this.displayName = displayName;
    if (initParams != null) {
      this.initParams = initParams.toArray(new WebInitParamMetaData[0]);
    }
    this.filterName = filterName;
    this.smallIcon = smallIcon;
    this.largeIcon = largeIcon;
    if (servletNames != null) {
      this.servletNames = servletNames.toArray(new String[0]);
    }
    if (value != null) {
      this.value = value.toArray(new String[0]);
    }
    if (urlPatterns != null) {
      this.urlPatterns = urlPatterns.toArray(new String[0]);
    }
    if (dispatcherTypes != null) {
      this.dispatcherTypes = dispatcherTypes.toArray(new DispatcherType[0]);
    }
    this.asyncSupported = asyncSupported;
    this.description = description;
    this.clazz = clazz;
  }

  public WebFilterMetaData(WebFilter anno, Class<? extends Filter> clazz) {
    if (anno != null) {
      displayName = anno.displayName();
      description = anno.description();
      initParams = WebInitParamMetaData.of(anno.initParams());
      filterName = anno.filterName();
      smallIcon = anno.smallIcon();
      largeIcon = anno.largeIcon();
      servletNames = anno.servletNames();
      value = anno.value();
      urlPatterns = anno.urlPatterns();
      dispatcherTypes = anno.dispatcherTypes();
      asyncSupported = anno.asyncSupported();
      this.clazz = clazz;
    }
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
    return dispatcherTypes;
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
    return initParams;
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
    return servletNames;
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
    return urlPatterns;
  }

  /**
   *
   * @return the value
   */
  public String[] getValue() {
    return value;
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
    this.clazz = requireNotNull(clazz, "");// FIXME MSG
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
    this.initParams = initParams;
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
    this.servletNames = servletNames;
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
    this.urlPatterns = urlPatterns;
  }


  /**
   *
   * @param value the value to set
   */
  protected void setValue(String[] value) {
    this.value = value;
  }


}

/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;

public class PersistenceUnitMetaData {

  private final String persistenceUnitName;
  private boolean excludeUnlistedClasses = true;
  private Set<ClassTransformer> transformers = new LinkedHashSet<>();
  private ClassLoader classLoader;
  private ClassLoader newTempClassLoader;
  private List<URL> jarFileUrls = new ArrayList<>();
  private String jtaDataSource;
  private String nonJtaDataSource;
  private Set<String> managedClassNames = new LinkedHashSet<>();
  private Set<String> mappingFileNames = new LinkedHashSet<>();
  private String persistenceProviderClassName;
  private URL persistenceUnitRootUrl;
  private String persistenceXMLSchemaVersion;
  private Properties properties = new Properties();
  private SharedCacheMode sharedCacheMode = SharedCacheMode.NONE;
  private PersistenceUnitTransactionType persistenceUnitTransactionType =
      PersistenceUnitTransactionType.JTA;
  private ValidationMode validationMode = ValidationMode.NONE;

  /**
   * @param persistenceUnitName
   */
  public PersistenceUnitMetaData(String persistenceUnitName) {
    super();
    this.persistenceUnitName = persistenceUnitName;
  }


  public void addTransformer(ClassTransformer transformer) {
    if (transformer != null) {
      transformers.add(transformer);
    }
  }


  public boolean excludeUnlistedClasses() {
    return excludeUnlistedClasses;
  }


  public ClassLoader getClassLoader() {
    return classLoader;
  }


  public List<URL> getJarFileUrls() {
    return new ArrayList<>(jarFileUrls);
  }


  public String getJtaDataSource() {
    return jtaDataSource;
  }


  public List<String> getManagedClassNames() {
    return new ArrayList<>(managedClassNames);
  }


  public List<String> getMappingFileNames() {
    return new ArrayList<>(mappingFileNames);
  }


  public ClassLoader getNewTempClassLoader() {
    return newTempClassLoader;
  }


  public String getNonJtaDataSource() {
    return nonJtaDataSource;
  }


  public String getPersistenceProviderClassName() {
    return persistenceProviderClassName;
  }


  public String getPersistenceUnitName() {
    return persistenceUnitName;
  }


  public URL getPersistenceUnitRootUrl() {
    return persistenceUnitRootUrl;
  }


  public String getPersistenceXMLSchemaVersion() {
    return persistenceXMLSchemaVersion;
  }


  public Properties getProperties() {
    return properties;
  }


  public SharedCacheMode getSharedCacheMode() {
    return sharedCacheMode;
  }


  public PersistenceUnitTransactionType getTransactionType() {
    return persistenceUnitTransactionType;
  }


  public ValidationMode getValidationMode() {
    return validationMode;
  }

  protected void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  protected void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
    this.excludeUnlistedClasses = excludeUnlistedClasses;
  }

  protected void setJarFileUrls(List<URL> jarFileUrls) {
    this.jarFileUrls.clear();
    if (jarFileUrls != null) {
      this.jarFileUrls.addAll(jarFileUrls);
    }
  }

  protected void setJtaDataSource(String jtaDataSource) {
    this.jtaDataSource = jtaDataSource;
  }

  protected void setManagedClassNames(Set<String> managedClassNames) {
    this.managedClassNames = managedClassNames;
  }

  protected void setMappingFileNames(Set<String> mappingFileNames) {
    this.mappingFileNames = mappingFileNames;
  }

  protected void setNewTempClassLoader(ClassLoader newTempClassLoader) {
    this.newTempClassLoader = newTempClassLoader;
  }

  protected void setNonJtaDataSource(String nonJtaDataSource) {
    this.nonJtaDataSource = nonJtaDataSource;
  }

  protected void setPersistenceProviderClassName(String persistenceProviderClassName) {
    this.persistenceProviderClassName = persistenceProviderClassName;
  }

  protected void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  protected void setPersistenceUnitTransactionType(
      PersistenceUnitTransactionType persistenceUnitTransactionType) {
    this.persistenceUnitTransactionType =
        defaultObject(persistenceUnitTransactionType, PersistenceUnitTransactionType.JTA);
  }

  protected void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
    this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
  }

  protected void setProperties(Properties properties) {
    this.properties = properties;
  }

  protected void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
    this.sharedCacheMode = sharedCacheMode;
  }

  protected void setTransformers(Set<ClassTransformer> transformers) {
    this.transformers = transformers;
  }

  protected void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

}

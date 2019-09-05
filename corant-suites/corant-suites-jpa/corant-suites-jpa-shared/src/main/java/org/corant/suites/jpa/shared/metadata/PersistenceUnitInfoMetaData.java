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
package org.corant.suites.jpa.shared.metadata;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.JPAConfig;

public class PersistenceUnitInfoMetaData implements PersistenceUnitInfo {

  private String name;
  private final String persistenceUnitName;
  private String version;
  private boolean excludeUnlistedClasses = true;
  private List<ClassTransformer> transformers = new ArrayList<>();
  private ClassLoader classLoader;
  private ClassLoader newTempClassLoader;
  private List<URL> jarFileUrls = new ArrayList<>();
  private String jtaDataSourceName;
  private DataSource jtaDataSource;
  private String nonJtaDataSourceName;
  private DataSource nonJtaDataSource;
  private List<String> managedClassNames = new ArrayList<>();
  private List<String> mappingFileNames = new ArrayList<>();
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
  public PersistenceUnitInfoMetaData(String persistenceUnitName) {
    super();
    this.persistenceUnitName = defaultTrim(persistenceUnitName);
  }

  public void addManagedClassName(String managedClassName) {
    managedClassNames.add(managedClassName);
  }

  @Override
  public void addTransformer(ClassTransformer transformer) {
    if (transformer != null) {
      transformers.add(transformer);
    }
  }

  public PersistenceUnitInfoMetaData configClassLoader(ClassLoader classLoader) {
    setClassLoader(classLoader);
    return this;
  }

  public PersistenceUnitInfoMetaData configDataSource(Function<String, DataSource> dsSupplier) {
    if (getPersistenceUnitTransactionType() == PersistenceUnitTransactionType.JTA) {
      setJtaDataSource(dsSupplier.apply(getJtaDataSourceName()));
    } else {
      setNonJtaDataSource(dsSupplier.apply(getNonJtaDataSourceName()));
    }
    return this;
  }

  public PersistenceUnitInfoMetaData configNewTempClassLoader(ClassLoader newTempClassLoader) {
    setNewTempClassLoader(newTempClassLoader);
    return this;
  }

  public PersistenceUnitInfoMetaData configProperties(Object key, Object value) {
    properties.put(key, value);
    return this;
  }

  public PersistenceUnitInfoMetaData configTransformers(ClassTransformer... classTransformers) {
    transformers.clear();
    for (ClassTransformer ctf : classTransformers) {
      transformers.add(ctf);
    }
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PersistenceUnitInfoMetaData other = (PersistenceUnitInfoMetaData) obj;
    if (persistenceUnitName == null) {
      if (other.persistenceUnitName != null) {
        return false;
      }
    } else if (!persistenceUnitName.equals(other.persistenceUnitName)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean excludeUnlistedClasses() {
    return excludeUnlistedClasses;
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public List<URL> getJarFileUrls() {
    return Collections.unmodifiableList(jarFileUrls);
  }

  @Override
  public DataSource getJtaDataSource() {
    return jtaDataSource;
  }

  public String getJtaDataSourceName() {
    return jtaDataSourceName;
  }

  @Override
  public List<String> getManagedClassNames() {
    return Collections.unmodifiableList(managedClassNames);
  }

  @Override
  public List<String> getMappingFileNames() {
    return Collections.unmodifiableList(mappingFileNames);
  }

  /**
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  @Override
  public ClassLoader getNewTempClassLoader() {
    return newTempClassLoader;
  }

  @Override
  public DataSource getNonJtaDataSource() {
    return nonJtaDataSource;
  }

  public String getNonJtaDataSourceName() {
    return nonJtaDataSourceName;
  }

  @Override
  public String getPersistenceProviderClassName() {
    return persistenceProviderClassName;
  }

  @Override
  public String getPersistenceUnitName() {
    return persistenceUnitName;
  }

  @Override
  public URL getPersistenceUnitRootUrl() {
    return persistenceUnitRootUrl;
  }

  public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
    return persistenceUnitTransactionType;
  }

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return persistenceXMLSchemaVersion;
  }

  @Override
  public Properties getProperties() {
    Properties pops = new Properties();
    properties.forEach(pops::put);
    return pops;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return sharedCacheMode;
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    return persistenceUnitTransactionType;
  }

  public List<ClassTransformer> getTransformers() {
    return Collections.unmodifiableList(transformers);
  }

  @Override
  public ValidationMode getValidationMode() {
    return validationMode;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (persistenceUnitName == null ? 0 : persistenceUnitName.hashCode());
    return result;
  }

  public boolean isExcludeUnlistedClasses() {
    return excludeUnlistedClasses;
  }

  public void putPropertity(String name, String value) {
    properties.put(name, value);
  }

  @Override
  public String toString() {
    return "PersistenceUnitInfoMetaData [persistenceUnitName=" + persistenceUnitName
        + ", persistenceProviderClassName=" + persistenceProviderClassName + "]";
  }

  public PersistenceUnitInfoMetaData with(Properties properties,
      PersistenceUnitTransactionType putt) {
    PersistenceUnitInfoMetaData newObj = new PersistenceUnitInfoMetaData(getPersistenceUnitName());
    newObj.setClassLoader(getClassLoader());
    newObj.setExcludeUnlistedClasses(isExcludeUnlistedClasses());
    newObj.setJarFileUrls(getJarFileUrls());
    newObj.setJtaDataSource(getJtaDataSource());
    newObj.setJtaDataSourceName(getJtaDataSourceName());
    newObj.setManagedClassNames(getManagedClassNames());
    newObj.setMappingFileNames(getMappingFileNames());
    newObj.setNewTempClassLoader(getNewTempClassLoader());
    newObj.setNonJtaDataSource(getNonJtaDataSource());
    newObj.setNonJtaDataSourceName(getNonJtaDataSourceName());
    newObj.setPersistenceProviderClassName(getPersistenceProviderClassName());
    newObj.setPersistenceUnitRootUrl(getPersistenceUnitRootUrl());
    newObj.setPersistenceUnitTransactionType(getPersistenceUnitTransactionType());
    newObj.setPersistenceXMLSchemaVersion(getPersistenceXMLSchemaVersion());
    newObj.setProperties(properties);
    newObj.setSharedCacheMode(getSharedCacheMode());
    newObj.setTransformers(getTransformers());
    newObj.setValidationMode(getValidationMode());
    newObj.setVersion(getVersion());
    newObj.setPersistenceUnitTransactionType(putt);
    return newObj;
  }

  protected void addJarFileUrl(URL url) {
    if (url != null && !jarFileUrls.contains(url)) {
      jarFileUrls.add(url);
    }
  }

  protected void addMappingFileName(String mappingFileName) {
    if (isNotBlank(mappingFileName) && !mappingFileNames.contains(mappingFileName)) {
      mappingFileNames.add(mappingFileName);
    }
  }

  protected void addTransFormer(ClassTransformer transformer) {
    if (transformer != null && !transformers.contains(transformer)) {
      transformers.add(transformer);
    }
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
      jarFileUrls.forEach(this::addJarFileUrl);
    }
  }

  protected void setJtaDataSource(DataSource jtaDataSource) {
    this.jtaDataSource = jtaDataSource;
  }

  protected void setJtaDataSourceName(String jtaDataSource) {
    jtaDataSourceName = jtaDataSource;
  }

  protected void setManagedClassNames(List<String> managedClassNames) {
    this.managedClassNames.clear();
    if (managedClassNames != null) {
      managedClassNames.forEach(this::addManagedClassName);
    }
  }

  protected void setMappingFileNames(List<String> mappingFileNames) {
    this.mappingFileNames.clear();
    if (mappingFileNames != null) {
      mappingFileNames.forEach(this::addMappingFileName);
    }
  }

  /**
   *
   * @param name the name to set
   */
  protected void setName(String name) {
    this.name = defaultTrim(name);
  }

  protected void setNewTempClassLoader(ClassLoader newTempClassLoader) {
    this.newTempClassLoader = newTempClassLoader;
  }

  protected void setNonJtaDataSource(DataSource nonJtaDataSource) {
    this.nonJtaDataSource = nonJtaDataSource;
  }

  protected void setNonJtaDataSourceName(String nonJtaDataSource) {
    nonJtaDataSourceName = nonJtaDataSource;
  }

  protected void setPersistenceProviderClassName(String persistenceProviderClassName) {
    this.persistenceProviderClassName = persistenceProviderClassName;
  }

  protected void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  protected void setPersistenceUnitTransactionType(
      PersistenceUnitTransactionType persistenceUnitTransactionType) {
    shouldBeTrue(
        persistenceUnitTransactionType == null
            || persistenceUnitTransactionType == PersistenceUnitTransactionType.JTA,
        "At present we only support JTA"); // FIXME
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

  protected void setTransformers(List<ClassTransformer> transformers) {
    this.transformers.clear();
    if (transformers != null) {
      transformers.forEach(this::addTransFormer);
    }
  }

  protected void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

  protected void setVersion(String version) {
    this.version = version;
  }

  void resolvePersistenceProvider() {
    if (isBlank(persistenceProviderClassName)) {
      PersistenceProvider pp = JPAConfig.resolvePersistenceProvider()
          .orElseThrow(() -> new CorantRuntimeException("Can not find jpa provider"));
      setPersistenceProviderClassName(getUserClass(pp.getClass()).getName());
    }
  }

}

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

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

public class PersistenceUnitMetaData implements PersistenceUnitInfo {

  private String persistenceUnitName;
  private boolean excludeUnlistedClasses = true;
  private Set<ClassTransformer> transformers = new LinkedHashSet<>();
  private ClassLoader classLoader;
  private ClassLoader newTempClassLoader;
  private Set<URL> jarFileUrls = new LinkedHashSet<>();
  private DataSource jtaDataSource;
  private DataSource nonJtaDataSource;
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


  @Override
  public void addTransformer(ClassTransformer transformer) {
    if (transformer != null) {
      transformers.add(transformer);
    }
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
    return new ArrayList<>(jarFileUrls);
  }

  @Override
  public DataSource getJtaDataSource() {
    return jtaDataSource;
  }

  @Override
  public List<String> getManagedClassNames() {
    return new ArrayList<>(managedClassNames);
  }

  @Override
  public List<String> getMappingFileNames() {
    return new ArrayList<>(mappingFileNames);
  }

  @Override
  public ClassLoader getNewTempClassLoader() {
    return newTempClassLoader;
  }

  @Override
  public DataSource getNonJtaDataSource() {
    return nonJtaDataSource;
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

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return persistenceXMLSchemaVersion;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return sharedCacheMode;
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    return persistenceUnitTransactionType;
  }

  @Override
  public ValidationMode getValidationMode() {
    return validationMode;
  }

}

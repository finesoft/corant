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
package org.corant.suites.jpa.shared;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import org.corant.kernel.normal.Names;
import org.corant.kernel.normal.Names.JndiNames;
import org.corant.shared.util.Resources;
import org.corant.shared.util.StringUtils;
import org.corant.suites.jpa.shared.metadata.PersistencePropertiesParser;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceXmlParser;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:32:07
 *
 */
public class JPAConfig {

  public static final String DFLT_PU_XML_LOCATION = "META-INF/persistence.xml";
  public static final String DFLT_ORM_XML_LOCATION = "META-INF/*JpaOrm.xml";
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/EntityManagerFactories";
  public static final String EMPTY_PU_NAME = StringUtils.EMPTY;

  public static final String JC_PREFIX = "jpa.";

  public static final String JCX_TAG = "persistence-pu";
  public static final String JCX_NME = "name";
  public static final String JCX_TRANS_TYP = "transaction-type";
  public static final String JCX_NON_JTA_DS = "non-jta-data-source";
  public static final String JCX_JTA_DS = "jta-data-source";
  public static final String JCX_PROVIDER = "provider";
  public static final String JCX_CLS = "class";
  public static final String JCX_MAP_FILE = "mapping-file";
  public static final String JCX_JAR_FILE = "jar-file";
  public static final String JCX_EX_UL_CLS = "exclude-unlisted-classes";
  public static final String JCX_VAL_MOD = "validation-mode";
  public static final String JCX_SHARE_CACHE_MOD = "shared-cache-mode";
  public static final String JCX_PROS = "properties";
  public static final String JCX_PRO = "property";
  public static final String JCX_PRO_NME = "name";
  public static final String JCX_PRO_VAL = "value";

  public static final String JC_PU_NME = "." + JCX_TAG + "." + JCX_NME;// persistence-pu.name
  public static final String JC_TRANS_TYP = "." + JCX_TRANS_TYP;
  public static final String JC_NON_JTA_DS = "." + JCX_NON_JTA_DS;
  public static final String JC_JTA_DS = "." + JCX_JTA_DS;
  public static final String JC_PROVIDER = "." + JCX_PROVIDER;
  public static final String JC_CLS = "." + JCX_CLS;
  public static final String JC_CLS_PKG = JC_CLS + "-packages";
  public static final String JC_MAP_FILE = "." + JCX_MAP_FILE;
  public static final String JC_MAP_FILE_PATH = "." + JCX_MAP_FILE + ".paths";
  public static final String JC_JAR_FILE = "." + JCX_JAR_FILE;
  public static final String JC_EX_UL_CLS = "." + JCX_EX_UL_CLS;
  public static final String JC_VAL_MOD = "." + JCX_VAL_MOD;
  public static final String JC_SHARE_CACHE_MOD = "." + JCX_SHARE_CACHE_MOD;
  public static final String JC_PRO = "." + "property";

  protected static final Logger logger = Logger.getLogger(JPAConfig.class.getName());

  public static Set<String> defaultPropertyNames(Config config) {
    String dfltPrefix = JC_PREFIX.substring(0, JC_PREFIX.length() - 1);
    String dfltJpaPropertyPrefix = dfltPrefix + JC_PRO + Names.NAME_SPACE_SEPARATORS;
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + JC_CLS);
    names.add(dfltPrefix + JC_CLS_PKG);
    names.add(dfltPrefix + JC_EX_UL_CLS);
    names.add(dfltPrefix + JC_JAR_FILE);
    names.add(dfltPrefix + JC_JTA_DS);
    names.add(dfltPrefix + JC_MAP_FILE);
    names.add(dfltPrefix + JC_MAP_FILE_PATH);
    names.add(dfltPrefix + JC_NON_JTA_DS);
    // jpa property
    for (String proNme : config.getPropertyNames()) {
      if (proNme.startsWith(dfltJpaPropertyPrefix)) {
        names.add(proNme);
      }
    }
    names.add(dfltPrefix + JC_PROVIDER);
    names.add(dfltPrefix + JC_SHARE_CACHE_MOD);
    names.add(dfltPrefix + JC_TRANS_TYP);
    names.add(dfltPrefix + JC_VAL_MOD);
    names.add(dfltPrefix + JC_PU_NME);
    return names;
  }

  public static Set<PersistenceUnitInfoMetaData> from(Config config) {
    Set<PersistenceUnitInfoMetaData> metaDatas = new HashSet<>();
    generateFromXml().forEach(metaDatas::add);
    generateFromConfig(config).forEach(u -> shouldBeTrue(metaDatas.add(u),
        "The persistence pu name %s is dup!", u.getPersistenceUnitName()));
    return metaDatas;
  }

  public static Optional<? extends PersistenceProvider> resolvePersistenceProvider() {
    return PersistenceProviderResolverHolder.getPersistenceProviderResolver()
        .getPersistenceProviders().stream().findFirst();
  }

  private static Set<PersistenceUnitInfoMetaData> generateFromConfig(Config config) {
    return PersistencePropertiesParser.parse(config);
  }

  private static Set<PersistenceUnitInfoMetaData> generateFromXml() {
    Set<PersistenceUnitInfoMetaData> cfgs = new HashSet<>();
    try {
      Resources.from(DFLT_PU_XML_LOCATION).map(r -> r.getUrl()).map(PersistenceXmlParser::parse)
          .flatMap(m -> m.stream()).forEach(m -> {
            shouldBeTrue(cfgs.add(m), "The persistence pu name %s is dup!",
                m.getPersistenceUnitName());
          });
    } catch (IOException e) {
      logger.warning(() -> String.format("Parse persistence pu meta data from %s error %s",
          DFLT_PU_XML_LOCATION, e.getMessage()));
    }
    return cfgs;
  }

}

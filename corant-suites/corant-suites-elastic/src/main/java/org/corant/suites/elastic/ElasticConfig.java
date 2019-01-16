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
package org.corant.suites.elastic;

import static org.corant.shared.util.StringUtils.isNoneBlank;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.kernel.util.ConfigUtils;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:54:10
 *
 */
public class ElasticConfig {

  public static final String PREFIX = "elastic.";
  public static final String ES_CLU_NOD = ".cluster-nodes";
  public static final String ES_ADD_PRO = ".property";

  private String clusterName;

  private String clusterNodes;

  private final Map<String, String> properties = new HashMap<>();

  public static Map<String, ElasticConfig> from(Config config) {
    Map<String, ElasticConfig> map = new HashMap<>();
    Map<String, List<String>> cfgNmes = ConfigUtils.getGroupConfigNames(config, PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      final ElasticConfig cfg = of(config, k, v);
      if (isNoneBlank(cfg.clusterName, cfg.clusterNodes)) {
        map.put(k, cfg);
      }
    });
    return map;
  }

  public static ElasticConfig of(Config config, String name, Collection<String> propertieNames) {
    final ElasticConfig cfg = new ElasticConfig();
    final String proPrefix = PREFIX + name + ES_ADD_PRO;
    final int proPrefixLen = proPrefix.length();
    Set<String> proCfgNmes = new HashSet<>();
    cfg.clusterName = name;
    propertieNames.forEach(pn -> {
      if (pn.endsWith(ES_CLU_NOD)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setClusterNodes);
      } else if (pn.startsWith(proPrefix) && pn.length() > proPrefixLen) {
        // handle properties
        proCfgNmes.add(pn);
      }
    });
    doParseProperties(config, proPrefix, proCfgNmes, cfg);
    return cfg;
  }

  static void doParseProperties(Config config, String proPrefix, Set<String> proCfgNmes,
      ElasticConfig esConfig) {
    if (!proCfgNmes.isEmpty()) {
      int len = proPrefix.length() + 1;
      for (String cfgNme : proCfgNmes) {
        config.getOptionalValue(cfgNme, String.class).ifPresent(s -> {
          String proName = cfgNme.substring(len);
          esConfig.properties.put(proName, s);
        });
      }
    }
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
    ElasticConfig other = (ElasticConfig) obj;
    if (clusterName == null) {
      if (other.clusterName != null) {
        return false;
      }
    } else if (!clusterName.equals(other.clusterName)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   *
   * @return the clusterNodes
   */
  public String getClusterNodes() {
    return clusterNodes;
  }

  /**
   *
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (clusterName == null ? 0 : clusterName.hashCode());
    return result;
  }

  protected Map<String, String> obtainProperties() {
    return properties;
  }

  /**
   *
   * @param clusterName the clusterName to set
   */
  protected void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   *
   * @param clusterNodes the clusterNodes to set
   */
  protected void setClusterNodes(String clusterNodes) {
    this.clusterNodes = clusterNodes;
  }

}

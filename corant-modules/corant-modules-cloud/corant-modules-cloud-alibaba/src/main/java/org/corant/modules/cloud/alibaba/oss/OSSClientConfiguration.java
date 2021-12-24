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
package org.corant.modules.cloud.alibaba.oss;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNoneBlank;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;
import org.eclipse.microprofile.config.Config;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.common.auth.RequestSigner;

/**
 * corant-modules-cloud-alibaba
 *
 * @author bingo 17:46:25
 *
 */
@ConfigKeyRoot(value = "corant.cloud.alibaba.oss", keyIndex = 4, ignoreNoAnnotatedItem = false)
public class OSSClientConfiguration extends ClientBuilderConfiguration
    implements DeclarativeConfig {

  private static final long serialVersionUID = -3160278341411416013L;

  protected boolean enableStorageService;

  protected String accessKeyId;

  protected String secretAccessKey;

  protected String securityToken;

  protected String endpoint;

  protected List<String> signerHandlerClasses = new ArrayList<>();

  protected Set<String> buckets = new HashSet<>();

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> defaultRequestHeaders = new LinkedHashMap<>();

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public Set<String> getBuckets() {
    return buckets;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public String getSecurityToken() {
    return securityToken;
  }

  public boolean isEnableStorageService() {
    return enableStorageService;
  }

  @Override
  public boolean isValid() {
    return isNoneBlank(endpoint, accessKeyId, secretAccessKey);
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    DeclarativeConfig.super.onPostConstruct(config, key);
    if (isNotEmpty(signerHandlerClasses)) {
      for (String cls : signerHandlerClasses) {
        Class<?> rscls = tryAsClass(cls);
        if (rscls != null && RequestSigner.class.isAssignableFrom(rscls)) {
          RequestSigner rs = (RequestSigner) resolve(rscls);
          signerHandlers.add(rs);
        }
      }
    }
    if (isNotEmpty(defaultRequestHeaders)) {
      defaultHeaders.putAll(defaultRequestHeaders);
    }
  }

}

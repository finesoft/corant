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
package org.corant.suites.microprofile.restclient;

import static org.corant.suites.cdi.Instances.select;
import java.util.logging.Logger;
import javax.ws.rs.Priorities;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;

/**
 * corant-suites-mp-restclient
 *
 * @author bingo 下午2:35:41
 */
public class MpRestClientBuilderListener implements RestClientBuilderListener {

  public static final String DFLT_CTX_RESOLER_KEY = "mp.restclient.default-context-resolver.enable";
  public static final boolean enableDefaultContextResolver = ConfigProvider.getConfig()
      .getOptionalValue(DFLT_CTX_RESOLER_KEY, Boolean.class).orElse(Boolean.TRUE);

  Logger logger = Logger.getLogger(this.getClass().toString());

  @Override
  public void onNewBuilder(RestClientBuilder builder) {
    if (enableDefaultContextResolver) {
      logger.fine("Register default mp context resolver to RestClientBuilder");
      builder.register(MpDefaultContextResolver.class, Priorities.USER);
    }
    select(MpRestClientBuilderConfigurator.class).stream().sorted(Sortable::compare)
        .forEach(x -> x.config(builder));
  }
}

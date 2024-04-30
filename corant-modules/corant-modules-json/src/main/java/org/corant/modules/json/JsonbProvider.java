/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.json;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:36:42
 */
@ApplicationScoped
public class JsonbProvider {

  @Inject
  protected Logger logger;

  protected void disposesJsonb(@Disposes @Any Jsonb jsonb) {
    try {
      jsonb.close();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Disposes Jsonb error!", e);
    }
  }

  @Produces
  @ApplicationScoped
  protected Jsonb jsonb(JsonbConfig jsonbConfig) {
    return JsonbBuilder.create(jsonbConfig);
  }

  @Produces
  @Dependent
  protected JsonbConfig jsonbConfig(Instance<JsonbConfigConfigurator> configurators) {
    final JsonbConfig jsonbConfig = new JsonbConfig();
    configurators.stream().sorted(Sortable::reverseCompare)
        .forEach(cfr -> cfr.configure(jsonbConfig));
    return jsonbConfig;
  }
}

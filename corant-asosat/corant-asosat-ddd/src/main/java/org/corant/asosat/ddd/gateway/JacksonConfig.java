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
package org.corant.asosat.ddd.gateway;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.corant.asosat.ddd.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午4:49:32
 *
 */
@ApplicationScoped
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonConfig implements ContextResolver<ObjectMapper> {

  private final ObjectMapper objectMapper;

  public JacksonConfig() throws Exception {
    objectMapper = JsonUtils.copyMapperForJs();
  }

  @Override
  public ObjectMapper getContext(Class<?> objectType) {
    return objectMapper;
  }
}

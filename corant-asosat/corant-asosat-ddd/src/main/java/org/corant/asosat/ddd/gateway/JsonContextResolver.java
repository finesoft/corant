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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.corant.suites.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-asosat-ddd
 * @author bingo 下午4:49:32
 */
@ApplicationScoped
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper objectMapperJs = JsonUtils.copyMapperForJs();

    private final ObjectMapper objectMapperRpc = JsonUtils.copyMapperForRpc();

    @Inject
    @Any
    Instance<JsonContextResolverConfigurator> configurator;

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        if (objectType.getName().endsWith("DTO")) {//FIXME DON 临时判断
            return objectMapperRpc;
        }
        return objectMapperJs;
    }

    @PostConstruct
    void onPostConstruct() {
        if (!configurator.isUnsatisfied()) {
            configurator.forEach(cfg -> cfg.config(objectMapperJs));
        }
    }
}

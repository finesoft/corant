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
package org.corant.modules.jaxrs.resteasy.patch;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.corant.config.Configs;
import org.corant.shared.ubiquity.Atomics;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.core.providerfactory.SortedKey;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * corant-modules-jaxrs-resteasy
 *
 * @author bingo 上午11:10:48
 *
 */
public abstract class AbstractImprovedJsonWriter<T> implements AsyncMessageBodyWriter<T> {

  protected static boolean cacheOriginalJsonWriter =
      Configs.getValue("corant.resteasy.patch.cache-original-json-writer", Boolean.TYPE, true);

  protected static Supplier<ResteasyJackson2Provider> cacheProvider =
      Atomics.strictAtomicInitializer(AbstractImprovedJsonWriter::resolveOriginalProvider);

  @SuppressWarnings("rawtypes")
  static ResteasyJackson2Provider resolveOriginalProvider() {
    List<SortedKey<MessageBodyWriter>> writers =
        ((ResteasyProviderFactoryImpl) ResteasyProviderFactory.getInstance()).getServerHelper()
            .getMessageBodyWriters().getPossible(MediaType.APPLICATION_JSON_TYPE);
    if (writers != null) {
      Optional<SortedKey<MessageBodyWriter>> op =
          writers.stream().filter(p -> p.getObj() instanceof ResteasyJackson2Provider).findFirst();
      if (op.isPresent()) {
        return (ResteasyJackson2Provider) op.get().getObj();
      }
    }
    return new ResteasyJackson2Provider();
  }

  protected ResteasyJackson2Provider internalWriter() {
    if (cacheOriginalJsonWriter) {
      return cacheProvider.get();
    } else {
      return resolveOriginalProvider();
    }
  }
}

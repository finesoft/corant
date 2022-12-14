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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;
import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.corant.shared.ubiquity.Experimental;
import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.AsyncOutputStream;

/**
 * corant-modules-jaxrs-resteasy
 * <p>
 * Class use to improve org.jboss.resteasy.plugins.providers.DefaultNumberWriter for output JSON
 * number.
 * <p>
 * The internal provider is ResteasyJackson2Provider, we just wrapped it and increase the
 * priority(3000)
 *
 * @author bingo 上午11:07:19
 *
 */
@Experimental
@Provider
@Priority(3000)
@Consumes({"application/json", "application/*+json", "text/json"})
@Produces({"application/json", "application/*+json", "text/json"})
public class ImprovedJsonWebNumberWriter extends AbstractImprovedJsonWriter<Number>
    implements AsyncMessageBodyWriter<Number> {

  @Override
  public CompletionStage<Void> asyncWriteTo(Number t, Class<?> type, Type genericType,
      Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
      AsyncOutputStream entityStream) {
    return internalWriter().asyncWriteTo(t, type, genericType, annotations, mediaType, httpHeaders,
        entityStream);
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType) {
    return Number.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(Number t, Class<?> type, Type genericType, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
      throws IOException, WebApplicationException {
    internalWriter().writeTo(t, type, genericType, annotations, mediaType, httpHeaders,
        entityStream);
  }

}

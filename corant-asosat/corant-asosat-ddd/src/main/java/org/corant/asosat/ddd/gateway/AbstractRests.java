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

import static org.corant.shared.util.StringUtils.split;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;

/**
 * @author bingo 下午5:51:09
 *
 */
@ApplicationScoped
@ApplicationServices
public abstract class AbstractRests {

  protected static final Map<Class<?>, String> cachedPaths = new ConcurrentHashMap<>();

  /**
   * 202
   *
   * @param obj
   * @return accepted
   */
  protected Response accepted() {
    return Response.accepted().build();
  }

  /**
   * 202
   *
   * @param obj
   * @return accepted
   */
  protected Response accepted(Object obj) {
    return Response.accepted(obj).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * 201
   *
   * @param id
   * @return created
   */
  protected Response created(Object id) {
    return created(URI.create(resolvePath() + "/get/" + id));
  }

  /**
   * 201
   *
   * @param location
   * @return created
   */
  protected Response created(URI location) {
    return Response.created(location).build();
  }

  /**
   * 204
   *
   * @return noContent
   */
  protected Response noContent() {
    return Response.noContent().build();
  }

  /**
   * 200
   *
   * @return ok
   */
  protected Response ok() {
    return Response.ok().type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * 200
   *
   * @param obj
   * @return ok
   */
  protected Response ok(Object obj) {
    return Response.ok(obj).type(MediaType.APPLICATION_JSON).build();
  }

  protected String parseMpFileName(MultivaluedMap<String, String> headers) {
    for (String name : split(headers.getFirst("Content-Disposition"), ";", true, true)) {
      if (name.startsWith("filename")) {
        String[] tmp = split(name, "=", true, true);
        String fileName = tmp[1].replaceAll("\"", "");
        return fileName;
      }
    }
    return "unnamed-" + System.currentTimeMillis();
  }

  protected String resolvePath() {
    return cachedPaths.computeIfAbsent(getClass(), (cls) -> {
      Annotation[] annotations = cls.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation instanceof Path) {
          Path pathAnnotation = (Path) annotation;
          return pathAnnotation.value();
        }
      }
      return "";
    });
  }
}

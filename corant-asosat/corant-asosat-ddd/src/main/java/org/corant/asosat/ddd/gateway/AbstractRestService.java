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

import java.net.URI;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;

/**
 * @author bingo 下午5:51:09
 *
 */
@ApplicationScoped
@ApplicationServices
public abstract class AbstractRestService {

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
}

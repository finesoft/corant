/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.exp.interfaces;

import static org.corant.shared.util.MapUtils.asMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.websocket.server.PathParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * corant-asosat-exp
 *
 * @author bingo 下午6:04:40
 *
 */
@Path("/client")
@ApplicationScoped
@Transactional
public class TestClient {

  @Inject
  @PersistenceContext(unitName = "dmmsPu")
  EntityManager em;

  @Path("/get/{id}/")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<Object, Object> get(@PathParam("id") String id) {
    return asMap("rep", em.toString());
  }
}

package org.corant.modules.microprofile.restclient;

import jakarta.ws.rs.ext.ContextResolver;
import org.corant.modules.json.Jsons;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-modules-microprofile-restclient
 *
 * @author sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  static final ObjectMapper om = Jsons.copyMapper();

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }

}

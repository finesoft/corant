package org.corant.modules.microprofile.restclient;

import javax.ws.rs.ext.ContextResolver;
import org.corant.suites.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-modules-microprofile-restclient
 *
 * @author sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  static final ObjectMapper om = JsonUtils.copyMapper();

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }

}

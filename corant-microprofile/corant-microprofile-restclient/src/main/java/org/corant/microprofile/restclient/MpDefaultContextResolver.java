package org.corant.microprofile.restclient;

import javax.ws.rs.ext.ContextResolver;
import org.corant.suites.json.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * cps-m2b <br>
 *
 * @auther sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  static ObjectMapper om;

  static {
    om = JsonUtils.copyMapper();
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }
}

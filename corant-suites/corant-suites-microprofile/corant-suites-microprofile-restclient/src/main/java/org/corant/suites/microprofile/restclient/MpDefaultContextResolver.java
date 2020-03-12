package org.corant.suites.microprofile.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.ext.ContextResolver;
import org.corant.suites.json.JsonUtils;

/**
 * cps-m2b <br>
 * @auther sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  final static ObjectMapper om = JsonUtils.copyMapper();

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }

}

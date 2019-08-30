package org.corant.suites.mp.restclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.corant.suites.json.JsonUtils;

import javax.ws.rs.ext.ContextResolver;

/**
 * cps-m2b <br>
 *
 * @auther sushuaihao 2019/8/30
 * @since
 */
public class MpDefaultContextResolver implements ContextResolver<ObjectMapper> {

  static ObjectMapper om;

  static {
    om = JsonUtils.copyMapperForRpc();
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return om;
  }
}

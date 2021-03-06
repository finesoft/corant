package org.corant.modules.microprofile.lra;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.ext.Provider;
import io.narayana.lra.filter.ClientLRARequestFilter;

/**
 * corant-modules-microprofile-lra
 *
 * @author sushuaihao 2019/11/22
 * @since
 */
@Provider
public class ClientLRARequestFilterExt extends ClientLRARequestFilter {
  @Override
  public void filter(ClientRequestContext context) {
    super.filter(context);
  }
}

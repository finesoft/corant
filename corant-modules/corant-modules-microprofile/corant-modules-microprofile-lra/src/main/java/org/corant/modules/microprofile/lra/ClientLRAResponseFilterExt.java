package org.corant.modules.microprofile.lra;

import java.io.IOException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.ext.Provider;
import io.narayana.lra.filter.ClientLRAResponseFilter;

/**
 * corant-modules-microprofile-lra
 *
 * @author sushuaihao 2019/11/22
 * @since
 */
@Provider
public class ClientLRAResponseFilterExt extends ClientLRAResponseFilter {
  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    super.filter(requestContext, responseContext);
  }
}

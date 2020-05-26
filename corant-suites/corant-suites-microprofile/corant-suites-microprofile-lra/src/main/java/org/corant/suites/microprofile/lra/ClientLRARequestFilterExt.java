package org.corant.suites.microprofile.lra;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.ext.Provider;
import io.narayana.lra.filter.ClientLRARequestFilter;

/**
 * tom-jerry <br>
 *
 * @auther sushuaihao 2019/11/22
 * @since
 */
@Provider
public class ClientLRARequestFilterExt extends ClientLRARequestFilter {
  @Override
  public void filter(ClientRequestContext context) {
    super.filter(context);
  }
}

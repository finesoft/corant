package org.corant.suites.jaxrs.resteasy;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import org.corant.suites.servlet.abstraction.AbstractCorsFilter;

@Provider
@PreMatching
@Priority(Priorities.AUTHORIZATION)
@ApplicationScoped
public class ResteasyCorsFilter extends AbstractCorsFilter {

}
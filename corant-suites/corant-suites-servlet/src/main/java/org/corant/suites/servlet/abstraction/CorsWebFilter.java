package org.corant.suites.servlet.abstraction;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebFilter;

//FIXME DON
@WebFilter(urlPatterns = "/*")
@ApplicationScoped
public class CorsWebFilter extends AbstractCorsFilter {

}

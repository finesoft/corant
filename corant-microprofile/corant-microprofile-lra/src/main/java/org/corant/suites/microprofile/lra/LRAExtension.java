package org.corant.suites.microprofile.lra;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import io.narayana.lra.filter.ClientLRARequestFilter;
import io.narayana.lra.filter.ClientLRAResponseFilter;
import io.narayana.lra.filter.FilterRegistration;
import io.narayana.lra.filter.ServerLRAFilter;
import io.narayana.lra.provider.ParticipantStatusOctetStreamProvider;

/**
 *
 * @auther sushuaihao 2019/11/21
 * @since
 */
public class LRAExtension implements Extension {

  void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event, BeanManager beanManager) {

    event.addAnnotatedType(beanManager.createAnnotatedType(ServerLRAFilter.class),
        ServerLRAFilter.class.getSimpleName());

    event.addAnnotatedType(beanManager.createAnnotatedType(ClientLRARequestFilter.class),
        ClientLRARequestFilter.class.getSimpleName());

    event.addAnnotatedType(beanManager.createAnnotatedType(ClientLRAResponseFilter.class),
        ClientLRAResponseFilter.class.getSimpleName());

    event.addAnnotatedType(beanManager.createAnnotatedType(FilterRegistration.class),
        FilterRegistration.class.getSimpleName());

    event.addAnnotatedType(
        beanManager.createAnnotatedType(ParticipantStatusOctetStreamProvider.class),
        ParticipantStatusOctetStreamProvider.class.getSimpleName());
  }
}

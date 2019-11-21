package org.corant.suites.microprofile.lra;

import io.narayana.lra.filter.ClientLRARequestFilter;
import io.narayana.lra.filter.ClientLRAResponseFilter;
import io.narayana.lra.filter.FilterRegistration;
import io.narayana.lra.filter.ServerLRAFilter;
import io.narayana.lra.provider.ParticipantStatusOctetStreamProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 *
 * @auther sushuaihao 2019/11/21
 * @since
 */
public class LRAExtension implements Extension {

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {

    afterBeanDiscovery.<ServerLRAFilter>addBean()
            .addTransitiveTypeClosure(ServerLRAFilter.class)
            .beanClass(ServerLRAFilter.class).scope(ApplicationScoped.class);
    afterBeanDiscovery.<ClientLRARequestFilter>addBean()
            .addTransitiveTypeClosure(ClientLRARequestFilter.class)
            .beanClass(ClientLRARequestFilter.class).scope(ApplicationScoped.class);
    afterBeanDiscovery.<FilterRegistration>addBean()
            .addTransitiveTypeClosure(FilterRegistration.class)
            .beanClass(FilterRegistration.class).scope(ApplicationScoped.class);
    afterBeanDiscovery.<ClientLRAResponseFilter>addBean()
            .addTransitiveTypeClosure(ClientLRAResponseFilter.class)
            .beanClass(ClientLRAResponseFilter.class).scope(ApplicationScoped.class);
    afterBeanDiscovery.<ParticipantStatusOctetStreamProvider>addBean()
            .addTransitiveTypeClosure(ParticipantStatusOctetStreamProvider.class)
            .beanClass(ParticipantStatusOctetStreamProvider.class).scope(ApplicationScoped.class);
  }
}

package org.corant.modules.microprofile.lra;

import static org.corant.shared.util.Objects.defaultObject;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.corant.config.Configs;
import org.corant.shared.util.Systems;
import io.narayana.lra.filter.ClientLRARequestFilter;
import io.narayana.lra.filter.ClientLRAResponseFilter;
import io.narayana.lra.filter.ServerLRAFilter;
import io.narayana.lra.provider.ParticipantStatusOctetStreamProvider;

/**
 * corant-modules-microprofile-lra
 *
 * @author sushuaihao 2019/11/21
 * @since
 */
public class LRAExtension implements Extension {

  /**
   * Key for looking up the config property that specifies which host a coordinator is running on
   */
  public static final String LRA_COORDINATOR_HOST_KEY = "lra.http.host";
  /**
   * Key for looking up the config property that specifies which port a coordinator is running on
   */
  public static final String LRA_COORDINATOR_PORT_KEY = "lra.http.port";

  LRAConfig config;

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery event, BeanManager beanManager) {}

  void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event, BeanManager beanManager) {

    config = defaultObject(Configs.resolveSingle(LRAConfig.class), LRAConfig.EMPTY);

    Systems.setProperty(LRA_COORDINATOR_PORT_KEY, String.valueOf(config.getPort()));
    Systems.setProperty(LRA_COORDINATOR_HOST_KEY, config.getHost());

    event.addAnnotatedType(beanManager.createAnnotatedType(ServerLRAFilter.class),
        ServerLRAFilter.class.getSimpleName());

    // event.addAnnotatedType(beanManager.createAnnotatedType(FilterRegistration.class),
    // FilterRegistration.class.getSimpleName()); FIXME version upgrade

    event.addAnnotatedType(
        beanManager.createAnnotatedType(ParticipantStatusOctetStreamProvider.class),
        ParticipantStatusOctetStreamProvider.class.getSimpleName());

    event.addAnnotatedType(beanManager.createAnnotatedType(ClientLRARequestFilterExt.class),
        ClientLRARequestFilter.class.getSimpleName());

    event.addAnnotatedType(beanManager.createAnnotatedType(ClientLRAResponseFilterExt.class),
        ClientLRAResponseFilter.class.getSimpleName());
  }
}

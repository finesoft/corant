package org.corant.suites.redis;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class RedissonExtension implements Extension {

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent) {


    }
}

package org.corant.suites.redis;

import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

@Dependent
public class RedissonClientProducer {

    @Inject
    @ConfigProperty(name = "redis.database")
    private Optional<Integer> database;

    @Inject
    @ConfigProperty(name = "redis.address")
    private String address;

    @Inject
    @ConfigProperty(name = "redis.password")
    private Optional<String> password;

    @Inject
    @ConfigProperty(name = "redis.timeout")
    private Optional<Integer> timeout;

    @Produces
    @ApplicationScoped
    public RedissonClient produce() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress("redis://" + address);
        password.ifPresent(serverConfig::setPassword);
        database.ifPresent(serverConfig::setDatabase);
        timeout.ifPresent(serverConfig::setTimeout);
        return Redisson.create(config);
    }

    void dispose(@Disposes RedissonClient client) {
        client.shutdown();
    }
}

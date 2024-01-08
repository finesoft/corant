package org.corant.modules.redis;

import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

/**
 * corant-modules-redis
 *
 * @author don
 */
@Dependent
public class RedissonClientProducer {

  @Inject
  @ConfigProperty(name = "corant.redis.database")
  protected Optional<Integer> database;

  @Inject
  @ConfigProperty(name = "corant.redis.address")
  protected String address;

  @Inject
  @ConfigProperty(name = "corant.redis.password")
  protected Optional<String> password;

  @Inject
  @ConfigProperty(name = "corant.redis.timeout")
  protected Optional<Integer> timeout;

  public String getAddress() {
    return address;
  }

  public Optional<Integer> getDatabase() {
    return database;
  }

  public Optional<String> getPassword() {
    return password;
  }

  public Optional<Integer> getTimeout() {
    return timeout;
  }

  @Produces
  @ApplicationScoped
  public Redisson produce() {
    Config config = new Config();
    SingleServerConfig serverConfig = config.useSingleServer();
    serverConfig.setAddress("redis://" + address);
    password.ifPresent(serverConfig::setPassword);
    database.ifPresent(serverConfig::setDatabase);
    timeout.ifPresent(serverConfig::setTimeout);
    return (Redisson) Redisson.create(config);
  }

  void dispose(@Disposes RedissonClient client) {
    client.shutdown();
  }
}

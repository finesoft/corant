package org.corant.config;

import org.corant.config.spi.ConfigVariableProcessor;
import org.corant.shared.util.Streams;
import org.eclipse.microprofile.config.Config;

import java.util.stream.Collectors;

/**
 * corant-root <br>
 *
 * @author sushuaihao 2020/12/30
 * @since
 */
public class DefaultCorantConfigRawValueProviderImpl
    implements CorantConfigResolver.CorantConfigRawValueProvider {
  Config config;

  public DefaultCorantConfigRawValueProviderImpl(Config config) {
    this.config = config;
  }

  @Override
  public String get(boolean eval, String key) {
    ConfigVariableProcessor processor =
        new ConfigVariableProcessor(
            Streams.streamOf(config.getConfigSources()).collect(Collectors.toList()));
    if (eval) {
      return processor.evalValue(key);
    } else {
      return processor.getValue(key);
    }
  }
}

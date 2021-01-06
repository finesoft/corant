package org.corant.config.spi;

import java.util.stream.Collectors;
import org.corant.config.CorantConfigResolver;
import org.corant.shared.util.Streams;
import org.eclipse.microprofile.config.Config;

/**
 * corant-root <br>
 *
 * @author sushuaihao 2020/12/30
 * @since
 */
public class ConfigRawValueProvider
    implements CorantConfigResolver.CorantConfigRawValueProvider {
  Config config;

  public ConfigRawValueProvider(Config config) {
    this.config = config;
  }

  @Override
  public String get(boolean eval, String key) {
    ConfigVariableProcessor processor = new ConfigVariableProcessor(
        Streams.streamOf(config.getConfigSources()).collect(Collectors.toList()));
    if (eval) {
      return processor.evalValue(key);
    } else {
      return processor.getValue(key);
    }
  }
}

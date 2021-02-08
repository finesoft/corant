package org.corant.suites.quartz.embeddable;

import org.corant.context.proxy.SerializableContextualMethodHandler;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
public class CorantJobMetaData {
  private final SerializableContextualMethodHandler method;
  private final String triggerKey;
  private final String triggerGroup;
  private final String cron;
  private final long initialDelaySeconds;

  public CorantJobMetaData(SerializableContextualMethodHandler method) {
    this.method = method;
    final CorantTrigger ann = method.getMethod().getAnnotation(CorantTrigger.class);
    this.cron = ann.cron();
    this.triggerKey = ann.key();
    this.triggerGroup = ann.group();
    this.initialDelaySeconds = ann.initialDelaySeconds();
  }

  public static CorantJobMetaData of(SerializableContextualMethodHandler method) {
    return new CorantJobMetaData(method);
  }

  public String getCron() {
    return cron;
  }

  public long getInitialDelaySeconds() {
    return initialDelaySeconds;
  }

  public SerializableContextualMethodHandler getMethod() {
    return method;
  }

  public String getTriggerGroup() {
    return triggerGroup;
  }

  public String getTriggerKey() {
    return triggerKey;
  }
}

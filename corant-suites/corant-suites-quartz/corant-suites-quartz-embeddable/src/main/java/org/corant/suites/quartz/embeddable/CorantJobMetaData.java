package org.corant.suites.quartz.embeddable;

import org.corant.context.proxy.ContextualMethodHandler;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
public class CorantJobMetaData {
  private final ContextualMethodHandler method;
  private final String triggerKey;
  private final String triggerGroup;
  private final String cron;
  private final long initialDelaySeconds;

  public CorantJobMetaData(ContextualMethodHandler method) {
    this.method = method;
    final CorantTrigger ann = method.getMethod().getAnnotation(CorantTrigger.class);
    cron = ann.cron();
    triggerKey = ann.key();
    triggerGroup = ann.group();
    initialDelaySeconds = ann.initialDelaySeconds();
  }

  public static CorantJobMetaData of(ContextualMethodHandler method) {
    return new CorantJobMetaData(method);
  }

  public String getCron() {
    return cron;
  }

  public long getInitialDelaySeconds() {
    return initialDelaySeconds;
  }

  public ContextualMethodHandler getMethod() {
    return method;
  }

  public String getTriggerGroup() {
    return triggerGroup;
  }

  public String getTriggerKey() {
    return triggerKey;
  }
}

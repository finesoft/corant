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
  private final long startDelaySeconds;
  private final long startAtEpochMilli;
  private final long endAtEpochMilli;
  private final int triggerPriority;

  public CorantJobMetaData(ContextualMethodHandler method) {
    this.method = method;
    final CorantTrigger ann = method.getMethod().getAnnotation(CorantTrigger.class);
    cron = ann.cron();
    triggerKey = ann.key();
    triggerGroup = ann.group();
    startDelaySeconds = ann.startDelaySeconds();
    triggerPriority = ann.triggerPriority();
    startAtEpochMilli = ann.startAtEpochMilli();
    endAtEpochMilli = ann.endAtEpochMilli();
  }

  public static CorantJobMetaData of(ContextualMethodHandler method) {
    return new CorantJobMetaData(method);
  }

  public String getCron() {
    return cron;
  }

  public long getEndAtEpochMilli() {
    return endAtEpochMilli;
  }

  public ContextualMethodHandler getMethod() {
    return method;
  }

  public long getStartAtEpochMilli() {
    return startAtEpochMilli;
  }

  public long getStartDelaySeconds() {
    return startDelaySeconds;
  }

  public String getTriggerGroup() {
    return triggerGroup;
  }

  public String getTriggerKey() {
    return triggerKey;
  }

  public int getTriggerPriority() {
    return triggerPriority;
  }
}

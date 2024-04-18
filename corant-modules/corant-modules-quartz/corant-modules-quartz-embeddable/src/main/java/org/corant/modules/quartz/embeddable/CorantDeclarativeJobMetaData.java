package org.corant.modules.quartz.embeddable;

import static org.corant.shared.util.Configurations.getAssembledConfigValue;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toLong;
import org.corant.context.proxy.ContextualMethodHandler;
import org.quartz.Trigger;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 */
public class CorantDeclarativeJobMetaData {
  private final ContextualMethodHandler method;
  private final String triggerKey;
  private final String triggerGroup;
  private final String cron;
  private final long startDelaySeconds;
  private final long startAtEpochMilli;
  private final long endAtEpochMilli;
  private final int triggerPriority;

  public CorantDeclarativeJobMetaData(ContextualMethodHandler method) {
    this.method = method;
    final CorantTrigger ann = method.getMethod().getAnnotation(CorantTrigger.class);
    cron = getAssembledConfigValue(ann.cron());
    triggerKey = getAssembledConfigValue(ann.key());
    triggerGroup = getAssembledConfigValue(ann.group());
    startDelaySeconds = toLong(getAssembledConfigValue(ann.startDelaySeconds()), -1L);
    triggerPriority =
        toInteger(getAssembledConfigValue(ann.triggerPriority()), Trigger.DEFAULT_PRIORITY);
    startAtEpochMilli = toLong(getAssembledConfigValue(ann.startAtEpochMilli()), -1L);
    endAtEpochMilli = toLong(getAssembledConfigValue(ann.endAtEpochMilli()), -1L);
  }

  public static CorantDeclarativeJobMetaData of(ContextualMethodHandler method) {
    return new CorantDeclarativeJobMetaData(method);
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

package org.corant.modules.quartz.embeddable;

import static org.corant.shared.util.Configurations.getAssembledConfigValue;
import static org.corant.shared.util.Strings.isNotBlank;
import org.corant.config.CorantConfigExpander;
import org.corant.context.proxy.ContextualMethodHandler;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
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
    if (isNotBlank(ann.cron()) && ann.cron().startsWith(CorantConfigExpander.MACRO_VAR_PREFIX)) {
      cron = getAssembledConfigValue(ann.cron());
    } else {
      cron = ann.cron();
    }
    triggerKey = ann.key();
    triggerGroup = ann.group();
    startDelaySeconds = ann.startDelaySeconds();
    triggerPriority = ann.triggerPriority();
    startAtEpochMilli = ann.startAtEpochMilli();
    endAtEpochMilli = ann.endAtEpochMilli();
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

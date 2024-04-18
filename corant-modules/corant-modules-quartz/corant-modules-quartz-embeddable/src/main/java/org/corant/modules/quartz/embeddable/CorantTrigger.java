package org.corant.modules.quartz.embeddable;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.quartz.Scheduler;
import org.quartz.Trigger;

/**
 * corant-modules-quartz-embeddable
 *
 * @author bingo 下午8:09:45
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface CorantTrigger {

  /**
   * The trigger cron expression string to base the schedule on.
   *
   * @return cron
   */
  String cron() default EMPTY;

  /**
   * The trigger end at the date of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   */
  String endAtEpochMilli() default "-1";

  /**
   * if only group be set , will be invalid. Required with #key() and key's value can't be "".
   *
   * @see #key()
   */
  String group() default Scheduler.DEFAULT_GROUP;

  /**
   * Trigger key, combined with the {@link #group()} to form a trigger unique identifier.
   */
  String key() default EMPTY;

  /**
   * The trigger start at the date of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   */
  String startAtEpochMilli() default "-1";

  /**
   * The trigger delay seconds start , if #startAtEpochMilli() be set. This will be invalid.
   *
   * @see #startAtEpochMilli()
   */
  String startDelaySeconds() default "-1";

  /**
   * Set the Trigger's priority. When more than one Trigger have the same * fire time, the scheduler
   * will fire the one with the highest priority * first.
   */
  String triggerPriority() default Trigger.DEFAULT_PRIORITY + "";
}

package org.corant.suites.quartz.embeddable;

import org.quartz.Scheduler;
import org.quartz.Trigger;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface CorantTrigger {
  String cron() default "";

  /**
   * end at the date of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   *
   * @return
   */
  long endAtEpochMilli() default -1;

  /**
   * if only group be set , will be invalid. Required with #key() and key's value can't be "".
   *
   * @see #key()
   * @return
   */
  String group() default Scheduler.DEFAULT_GROUP;

  String key() default "";

  /**
   * start at the date of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   *
   * @return
   */
  long startAtEpochMilli() default -1;

  /**
   * delay seconds start , if #startAtEpochMilli() be set. This will be invalid.
   *
   * @see #startAtEpochMilli()
   * @return
   */
  long startDelaySeconds() default -1;

  /**
   * Set the Trigger's priority. When more than one Trigger have the same * fire time, the scheduler
   * will fire the one with the highest priority * first.
   *
   * @return
   */
  int triggerPriority() default Trigger.DEFAULT_PRIORITY;
}

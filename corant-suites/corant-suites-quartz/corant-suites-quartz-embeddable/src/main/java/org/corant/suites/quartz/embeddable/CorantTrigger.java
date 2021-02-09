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

  long endAtEpochMilli() default -1;

  String group() default Scheduler.DEFAULT_GROUP;

  String key() default "";

  long startAtEpochMilli() default -1;

  long startDelaySeconds() default -1;

  /**
   * Set the Trigger's priority. When more than one Trigger have the same * fire time, the scheduler
   * will fire the one with the highest priority * first.
   *
   * @return
   */
  int triggerPriority() default Trigger.DEFAULT_PRIORITY;
}

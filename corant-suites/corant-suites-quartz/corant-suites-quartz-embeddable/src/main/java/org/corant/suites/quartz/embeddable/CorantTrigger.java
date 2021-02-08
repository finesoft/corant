package org.corant.suites.quartz.embeddable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Documented
public @interface CorantTrigger {
  String key();

  String group();

  String cron() default "";

  long initialDelaySeconds() default -1;
}

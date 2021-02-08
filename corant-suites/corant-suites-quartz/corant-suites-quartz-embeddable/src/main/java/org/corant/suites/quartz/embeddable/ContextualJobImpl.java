package org.corant.suites.quartz.embeddable;

import org.corant.context.proxy.SerializableContextualMethodHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.lang.reflect.InvocationTargetException;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/1/19
 * @since
 */
public class ContextualJobImpl implements Job {

  private SerializableContextualMethodHandler methodHandler;

  public ContextualJobImpl(SerializableContextualMethodHandler methodHandler) {
    this.methodHandler = methodHandler;
  }

  @Override
  public void execute(JobExecutionContext context) {
    try {
      this.methodHandler.invoke();
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}

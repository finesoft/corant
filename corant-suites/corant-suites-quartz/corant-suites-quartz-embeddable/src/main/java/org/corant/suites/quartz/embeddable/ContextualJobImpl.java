package org.corant.suites.quartz.embeddable;

import java.lang.reflect.InvocationTargetException;
import org.corant.context.proxy.ContextualMethodHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * corant-suites-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/19
 * @since
 */
public class ContextualJobImpl implements Job {

  private ContextualMethodHandler methodHandler;

  public ContextualJobImpl(ContextualMethodHandler methodHandler) {
    this.methodHandler = methodHandler;
  }

  @Override
  public void execute(JobExecutionContext context) {
    try {
      methodHandler.invoke();
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}

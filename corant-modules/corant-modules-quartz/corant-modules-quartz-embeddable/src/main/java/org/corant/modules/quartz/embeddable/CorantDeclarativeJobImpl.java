package org.corant.modules.quartz.embeddable;

import java.lang.reflect.InvocationTargetException;
import org.corant.context.proxy.ContextualMethodHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/19
 * @since
 */
public class CorantDeclarativeJobImpl implements Job {

  private final ContextualMethodHandler methodHandler;

  public CorantDeclarativeJobImpl(ContextualMethodHandler methodHandler) {
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

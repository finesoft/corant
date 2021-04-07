package org.corant.modules.quartz.embeddable;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.context.CDIs;
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

  static final Logger logger = Logger.getLogger(CorantDeclarativeJobImpl.class.getName());

  private final ContextualMethodHandler methodHandler;

  public CorantDeclarativeJobImpl(ContextualMethodHandler methodHandler) {
    this.methodHandler = methodHandler;
  }

  @Override
  public void execute(JobExecutionContext context) {
    try {
      if (CDIs.isEnabled()) {
        methodHandler.invoke();
      } else {
        logger.warning(() -> "The CDI container was disabled!");
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      logger.log(Level.SEVERE, e, () -> "Corant job exectue occurred error!");
    }
  }
}

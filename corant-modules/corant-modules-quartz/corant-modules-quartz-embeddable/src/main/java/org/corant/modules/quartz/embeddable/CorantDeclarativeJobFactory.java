package org.corant.modules.quartz.embeddable;

import org.corant.context.proxy.ContextualMethodHandler;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/2/5
 * @since
 */
public class CorantDeclarativeJobFactory implements JobFactory {

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
    JobDetail jobDetail = bundle.getJobDetail();
    return new CorantDeclarativeJobImpl((ContextualMethodHandler) jobDetail.getJobDataMap()
        .get(String.valueOf(jobDetail.getKey())));
  }
}

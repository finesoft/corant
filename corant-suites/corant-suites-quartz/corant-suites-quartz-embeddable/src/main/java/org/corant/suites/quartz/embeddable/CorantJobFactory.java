package org.corant.suites.quartz.embeddable;

import javax.enterprise.context.ApplicationScoped;
import org.corant.context.proxy.ContextualMethodHandler;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/2/5
 * @since
 */
@ApplicationScoped
public class CorantJobFactory implements JobFactory {

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
    JobDetail jobDetail = bundle.getJobDetail();
    return new ContextualJobImpl((ContextualMethodHandler) jobDetail.getJobDataMap()
        .get(String.valueOf(jobDetail.getKey())));
  }
}

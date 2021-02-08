package org.corant.suites.quartz.embeddable;

import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.util.Sets;
import org.corant.shared.util.Strings;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
@ApplicationScoped
public class CorantJobManager {

  protected final Set<CorantJobMetaData> jobMetaDatas = Sets.newConcurrentHashSet();
  @Inject protected CorantJobExtension extension;
  @Inject
  CorantJobFactory cdiJobFactory;
  private Scheduler sched;

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv)
      throws SchedulerException {
    SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
    sched = schedFact.getScheduler();
    sched.setJobFactory(cdiJobFactory);
    sched.start();
    for (final CorantJobMetaData metaData : jobMetaDatas) {
      JobDetail job = newJob(ContextualJobImpl.class).build();
      job.getJobDataMap().put(String.valueOf(job.getKey()), metaData.getMethod());
      TriggerBuilder<Trigger> triggerBuilder =
          newTrigger().withIdentity(metaData.getTriggerKey(), metaData.getTriggerGroup());
      if (metaData.getInitialDelaySeconds() > 0) {
        triggerBuilder.startAt(
            Date.from(Instant.now().plusSeconds(metaData.getInitialDelaySeconds())));
      } else {
        triggerBuilder.startNow();
      }
      if (Strings.isNotBlank(metaData.getCron())) {
        triggerBuilder.withSchedule(cronSchedule(metaData.getCron()));
      }
      Trigger trigger = triggerBuilder.build();
      sched.scheduleJob(job, trigger);
    }
  }

  @PostConstruct
  protected void postConstruct() {
    extension.getJobMethods().stream().map(CorantJobMetaData::of).forEach(jobMetaDatas::add);
  }
}

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
  @Inject CorantJobFactory cdiJobFactory;
  private Scheduler sched;

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv)
      throws SchedulerException {
    SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
    sched = schedulerFactory.getScheduler();
    sched.setJobFactory(cdiJobFactory);
    sched.start();
    for (final CorantJobMetaData metaData : jobMetaDatas) {
      JobDetail job = newJob(ContextualJobImpl.class).build();
      job.getJobDataMap().put(String.valueOf(job.getKey()), metaData.getMethod());
      TriggerBuilder<Trigger> triggerBuilder = newTrigger();
      triggerBuilder.withPriority(metaData.getTriggerPriority());
      if (Strings.isNotBlank(metaData.getTriggerKey())) {
        triggerBuilder.withIdentity(metaData.getTriggerKey(), metaData.getTriggerGroup());
      }
      if (metaData.getStartDelaySeconds() > 0 || metaData.getStartAtEpochMilli() > 0) {
        if (metaData.getStartDelaySeconds() > 0) {
          triggerBuilder.startAt(
              Date.from(Instant.now().plusSeconds(metaData.getStartDelaySeconds())));
        }
        if (metaData.getStartAtEpochMilli() > 0) {
          triggerBuilder.startAt(Date.from(Instant.ofEpochMilli(metaData.getStartAtEpochMilli())));
        }
      } else {
        triggerBuilder.startNow();
      }
      if (metaData.getEndAtEpochMilli() > 0) {
        triggerBuilder.endAt(new Date(metaData.getEndAtEpochMilli()));
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

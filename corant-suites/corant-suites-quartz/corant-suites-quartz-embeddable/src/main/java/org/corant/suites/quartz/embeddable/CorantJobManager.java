package org.corant.suites.quartz.embeddable;

import static org.corant.shared.util.Strings.isNotBlank;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Atomics;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * corant-suites-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
@ApplicationScoped
public class CorantJobManager {

  @Inject
  Logger logger;

  @Inject
  protected CorantJobExtension extension;

  @Inject
  protected CorantJobFactory jobFactory;

  @Inject
  @ConfigProperty(name = "quartz.scheduler.shutdown.wait-for-jobs-complete", defaultValue = "false")
  protected boolean waitForJobsToComplete;

  @Inject
  @ConfigProperty(name = "quartz.scheduler.start-delayed")
  protected Optional<Integer> startDelayed;

  protected Supplier<Scheduler> schedulerSupplier = Atomics.atomicOneOffInitializer(() -> {
    try {
      SchedulerFactory schedulerFactory = new StdSchedulerFactory();
      Scheduler scheduler = schedulerFactory.getScheduler();
      scheduler.setJobFactory(jobFactory);
      if (startDelayed.isEmpty()) {
        scheduler.start();
      } else {
        scheduler.startDelayed(startDelayed.get());
      }
      return scheduler;
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  });

  public Scheduler getScheduler() {
    return schedulerSupplier.get();
  }

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv)
      throws SchedulerException {
    for (final CorantJobMetaData metaData : extension.getJobMetaDatas()) {
      JobDetail job = newJob(ContextualJobImpl.class).build();
      job.getJobDataMap().put(String.valueOf(job.getKey()), metaData.getMethod());
      TriggerBuilder<Trigger> triggerBuilder = newTrigger();
      triggerBuilder.withPriority(metaData.getTriggerPriority());
      if (isNotBlank(metaData.getTriggerKey())) {
        triggerBuilder.withIdentity(metaData.getTriggerKey(), metaData.getTriggerGroup());
      }
      if (metaData.getStartDelaySeconds() > 0 || metaData.getStartAtEpochMilli() > 0) {
        if (metaData.getStartDelaySeconds() > 0) {
          triggerBuilder
              .startAt(Date.from(Instant.now().plusSeconds(metaData.getStartDelaySeconds())));
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
      if (isNotBlank(metaData.getCron())) {
        triggerBuilder.withSchedule(cronSchedule(metaData.getCron()));
      }
      Trigger trigger = triggerBuilder.build();
      getScheduler().scheduleJob(job, trigger);
    }
  }

  protected void onPreContainerStopEvent(@Observes PreContainerStopEvent pse) {
    try {
      if (getScheduler() != null && !getScheduler().isShutdown()) {
        getScheduler().shutdown(waitForJobsToComplete);
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

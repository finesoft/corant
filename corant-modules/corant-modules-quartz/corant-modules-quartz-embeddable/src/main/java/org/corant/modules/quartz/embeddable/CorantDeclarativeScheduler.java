package org.corant.modules.quartz.embeddable;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyAsyncEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Systems;
import org.corant.shared.util.Threads;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

/**
 * corant-modules-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
@ApplicationScoped
public class CorantDeclarativeScheduler {

  public static final String SCHEDULER_NAME = Names.CORANT_PREFIX + "declarative.scheduler";

  public static final String JOB_THREAD_PREFIX = Names.CORANT_PREFIX + "declarative.scheduler.job";

  protected static final DirectSchedulerFactory schedulerFactory =
      DirectSchedulerFactory.getInstance();

  @Inject
  protected Logger logger;

  @Inject
  protected CorantSchedulerExtension extension;

  @Inject
  @ConfigProperty(name = "corant.quartz.declarative.scheduler.enable", defaultValue = "true")
  protected boolean enable;

  @Inject
  @ConfigProperty(name = "corant.quartz.declarative.scheduler.job.threads")
  protected Optional<Integer> jobThreads;

  @Inject
  @ConfigProperty(name = "corant.quartz.declarative.scheduler.shutdown.wait-for-jobs-complete",
      defaultValue = "true")
  protected boolean waitForJobsToComplete;

  @Inject
  @ConfigProperty(name = "corant.quartz.declarative.scheduler.start-delayed",
      defaultValue = "PT16S")
  protected Optional<Duration> startDelayed;

  protected Scheduler scheduler;

  public synchronized Scheduler getQuartzScheduler() {
    return scheduler;
  }

  public boolean isRunning() throws SchedulerException {
    return getQuartzScheduler() != null && !getQuartzScheduler().isInStandbyMode();
  }

  public void resume(Duration delay) {
    try {
      if (getQuartzScheduler() != null && !isRunning()) {
        if (delay == null) {
          getQuartzScheduler().start();
        } else {
          Threads.delayRunInDaemonx(delay, getQuartzScheduler()::start);
        }
        logger.info(() -> "Start the bulit-in declarative job scheduler!");
      }
    } catch (SchedulerException ex) {
      throw new CorantRuntimeException("Could not start Quartz Scheduler", ex);
    }
  }

  public void suspend() {
    if (getQuartzScheduler() != null) {
      try {
        getQuartzScheduler().standby();
        logger.info(() -> "Suspended the bulit-in declarative jobs!");
      } catch (SchedulerException ex) {
        throw new CorantRuntimeException(ex,
            "Can't suspend the bulit-in declarative job scheduler!");
      }
    }
  }

  protected synchronized void initializeDeclarativeScheduler() {
    try {
      logger.info(() -> "Initialize the bulit-in declarative job scheduler!");
      SimpleThreadPool stp =
          new SimpleThreadPool(max(jobThreads.orElse(0), Systems.getCPUs()), Thread.NORM_PRIORITY);
      stp.setThreadNamePrefix(JOB_THREAD_PREFIX);
      schedulerFactory.createScheduler(SCHEDULER_NAME, SCHEDULER_NAME, stp, new RAMJobStore());
      scheduler = schedulerFactory.getScheduler(SCHEDULER_NAME);
      scheduler.setJobFactory(new CorantDeclarativeJobFactory());
      SchedulerRepository.getInstance().remove(SCHEDULER_NAME);
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected void onPostCorantReadyEvent(@ObservesAsync final PostCorantReadyAsyncEvent adv)
      throws SchedulerException {
    if (!enable) {
      logger.info(() -> "The bulit-in declarative job scheduler is disabled!");
      return;
    }
    if (isNotEmpty(extension.getDeclarativeJobMetaDatas())) {
      initializeDeclarativeScheduler();
      resume(startDelayed.orElse(null));
      for (final CorantDeclarativeJobMetaData metaData : extension.getDeclarativeJobMetaDatas()) {
        JobDetail job = newJob(CorantDeclarativeJobImpl.class).build();
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
            triggerBuilder
                .startAt(Date.from(Instant.ofEpochMilli(metaData.getStartAtEpochMilli())));
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
        getQuartzScheduler().scheduleJob(job, trigger);
      }
    }
  }

  protected void onPreContainerStopEvent(@Observes PreContainerStopEvent pse) {
    try {
      if (getQuartzScheduler() != null && !getQuartzScheduler().isShutdown()) {
        logger.info(() -> "Shutdown the bulit-in declarative job scheduler!");
        getQuartzScheduler().shutdown(waitForJobsToComplete);
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

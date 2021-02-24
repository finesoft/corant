package org.corant.suites.quartz.embeddable;

import static org.corant.shared.util.Strings.isNotBlank;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

/**
 * corant-suites-quartz-embeddable <br>
 *
 * @author sushuaihao 2021/1/22
 * @since
 */
@ApplicationScoped
public class CorantJobManager {

  public static final String DECLARATIVE_SCHEDULER_NAME =
      Names.CORANT_PREFIX + "declarative.scheduler";

  public static final String DECLARATIVE_SCHEDULER_JOB_THREAD_PREFIX =
      Names.CORANT_PREFIX + "declarative.scheduler.job";

  protected static final DirectSchedulerFactory schedulerFactory =
      DirectSchedulerFactory.getInstance();

  @Inject
  Logger logger;

  @Inject
  protected CorantJobExtension extension;

  @Inject
  @ConfigProperty(name = "quartz.scheduler.declarative.job.threads", defaultValue = "10")
  protected Integer declarativeJobThreads;

  @Inject
  @ConfigProperty(name = "quartz.scheduler.shutdown.wait-for-jobs-complete", defaultValue = "false")
  protected boolean waitForJobsToComplete;

  @Inject
  @ConfigProperty(name = "quartz.scheduler.start-delayed")
  protected Optional<Integer> startDelayed;

  protected Scheduler declarativeScheduler;

  public DirectSchedulerFactory getSchedulerFactory() {
    return schedulerFactory;
  }

  protected Scheduler getDeclarativeScheduler() {
    return declarativeScheduler;
  }

  protected synchronized void initializeDeclarativeScheduler() {
    try {
      declarativeScheduler = schedulerFactory.getScheduler(DECLARATIVE_SCHEDULER_NAME);
      if (declarativeScheduler == null) {
        SimpleThreadPool stp = new SimpleThreadPool(declarativeJobThreads, Thread.NORM_PRIORITY);
        stp.setThreadNamePrefix(DECLARATIVE_SCHEDULER_JOB_THREAD_PREFIX);
        schedulerFactory.createScheduler(DECLARATIVE_SCHEDULER_NAME, DECLARATIVE_SCHEDULER_NAME,
            stp, new RAMJobStore());
        declarativeScheduler = schedulerFactory.getScheduler(DECLARATIVE_SCHEDULER_NAME);
        declarativeScheduler.setJobFactory(new CorantDeclarativeJobFactory());
      }
      if (!declarativeScheduler.isStarted()) {
        if (startDelayed.isEmpty()) {
          declarativeScheduler.start();
        } else {
          declarativeScheduler.startDelayed(startDelayed.get());
        }
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    initializeDeclarativeScheduler();
  }

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv)
      throws SchedulerException {
    for (final CorantJobMetaData metaData : extension.getJobMetaDatas()) {
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
      getDeclarativeScheduler().scheduleJob(job, trigger);
    }
  }

  protected void onPreContainerStopEvent(@Observes PreContainerStopEvent pse) {
    try {
      if (getDeclarativeScheduler() != null && !getDeclarativeScheduler().isShutdown()) {
        getDeclarativeScheduler().shutdown(waitForJobsToComplete);
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

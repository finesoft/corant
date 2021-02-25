package org.corant.suites.quartz.embeddable;

import static org.corant.shared.util.Assertions.shouldBeTrue;
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
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.suites.datasource.shared.DataSourceService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;

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

  public static final String DURABLE_SCHEDULER_NAME = Names.CORANT_PREFIX + "durable.scheduler";

  public static final String DURABLE_SCHEDULER_JOB_THREAD_PREFIX =
      Names.CORANT_PREFIX + "durable.scheduler.job";

  public static final String DURABLE_SCHEDULER_DS =
      Names.CORANT_PREFIX + "durable.scheduler.datasource";

  protected static final DirectSchedulerFactory schedulerFactory =
      DirectSchedulerFactory.getInstance();

  @Inject
  Logger logger;

  @Inject
  protected CorantJobExtension extension;

  @Inject
  @Any
  protected Instance<DataSourceService> dataSourceService;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.declarative.job.threads", defaultValue = "10")
  protected Integer declarativeJobThreads;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.durable.job.threads", defaultValue = "10")
  protected Integer durableJobThreads;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.durable.job.store.datasource")
  protected Optional<String> durableJobStoreDataSource;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.durable.job.store.table-prefix",
      defaultValue = "QRTZ_")
  protected String durableJobStoreTablePrefix;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.durable.job.store.instance-id",
      defaultValue = DURABLE_SCHEDULER_JOB_THREAD_PREFIX)
  protected String durableJobStoreInstanceId;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.shutdown.wait-for-jobs-complete",
      defaultValue = "false")
  protected boolean waitForJobsToComplete;

  @Inject
  @ConfigProperty(name = "quartz.builtin.scheduler.start-delayed")
  protected Optional<Integer> startDelayed;

  public Scheduler getDurableScheduler() {
    try {
      return schedulerFactory.getScheduler(DURABLE_SCHEDULER_NAME);
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public DirectSchedulerFactory getSchedulerFactory() {
    return schedulerFactory;
  }

  protected Scheduler getDeclarativeScheduler() {
    try {
      return schedulerFactory.getScheduler(DECLARATIVE_SCHEDULER_NAME);
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected synchronized void initializeDeclarativeScheduler() {
    try {
      Scheduler declarativeScheduler = schedulerFactory.getScheduler(DECLARATIVE_SCHEDULER_NAME);
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

  protected synchronized void initializeDurableScheduler() {
    try {
      if (durableJobStoreDataSource.isPresent()) {
        shouldBeTrue(dataSourceService.isResolvable(), "Can not found any data source service");
        final String ds = durableJobStoreDataSource.get();
        Scheduler durableScheduler = schedulerFactory.getScheduler(DURABLE_SCHEDULER_NAME);
        if (durableScheduler == null) {
          DBConnectionManager.getInstance().addConnectionProvider(DURABLE_SCHEDULER_DS,
              new CorantConnectionProvider(() -> dataSourceService.get().getManaged(ds)));
          JobStoreTX jdbcJobStore = new JobStoreTX();
          jdbcJobStore.setDataSource(DURABLE_SCHEDULER_DS);
          jdbcJobStore.setTablePrefix(durableJobStoreTablePrefix);
          jdbcJobStore.setInstanceId(durableJobStoreInstanceId);
          SimpleThreadPool stp = new SimpleThreadPool(durableJobThreads, Thread.NORM_PRIORITY);
          stp.setThreadNamePrefix(DURABLE_SCHEDULER_JOB_THREAD_PREFIX);
          schedulerFactory.createScheduler(DURABLE_SCHEDULER_NAME, DURABLE_SCHEDULER_NAME, stp,
              jdbcJobStore);
          durableScheduler = schedulerFactory.getScheduler(DECLARATIVE_SCHEDULER_NAME);
        }
        if (!durableScheduler.isStarted()) {
          if (startDelayed.isEmpty()) {
            durableScheduler.start();
          } else {
            durableScheduler.startDelayed(startDelayed.get());
          }
        }
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @PostConstruct
  protected void onPostConstruct() {
    initializeDeclarativeScheduler();
    initializeDurableScheduler();
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
        if (getDeclarativeScheduler().isShutdown()) {
          SchedulerRepository.getInstance().remove(DECLARATIVE_SCHEDULER_NAME);
        }
      }
      if (getDurableScheduler() != null && !getDurableScheduler().isShutdown()) {
        getDurableScheduler().shutdown(waitForJobsToComplete);
        if (getDurableScheduler().isShutdown()) {
          SchedulerRepository.getInstance().remove(DURABLE_SCHEDULER_NAME);
        }
      }
    } catch (SchedulerException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

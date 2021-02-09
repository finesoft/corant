package org.corant.suites.quartz.embeddable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Logger;

/**
 * config-tck <br>
 *
 * @author sushuaihao 2021/2/5
 * @since
 */
@Named("hello")
@ApplicationScoped
public class TestJobService {
  @Inject Logger logger;

  @CorantTrigger(key = "job111", group = "jobG111", cron = "0/20 * * * * ?", startDelaySeconds = 5)
  public void job_1() {
    logger.info("job_1====job_1====job_1====job_1");
  }

  @CorantTrigger()
  public void job_2() {
    logger.info("job_2====job_2====job_2====job_2");
  }

  @CorantTrigger(key = "job__3")
  public void job_3() {
    logger.info("job_3====job_3====job_3====job_3");
  }

  @CorantTrigger(key = "job__4")
  public void job_4() {
    logger.info("job_4====job_4====job_4====job_4");
  }

  @CorantTrigger(group = "job_group_5", startAtEpochMilli = 1612853711000l)
  public void job_5() {
    logger.info("job_5====job_5====job_5====job_5");
  }

  @CorantTrigger(group = "job_group_6", startAtEpochMilli = 1612854611000L, cron = "0/10 * * * * ?")
  public void job_6() {
    logger.info("job_6====job_6====job_6====job_6");
  }

  @CorantTrigger(
      startAtEpochMilli = 1612856398000L,
      startDelaySeconds = 300,
      cron = "0/6 * * * * ?",
      endAtEpochMilli = 1612857011000L)
  public void job_7() {
    logger.info("job_7====job_7====job_7====job_7");
  }
}

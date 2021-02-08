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

  @CorantTrigger(key = "job111", group = "jobG111", cron = "0/2 * * * * ?", initialDelaySeconds = 5)
  public void job_1() {
    logger.info("job_1====job_1====job_1====job_1");
  }

  @CorantTrigger(key = "job222", group = "jobG222")
  public void job_2() {
    logger.info("job_2====job_2====job_2====job_2");
  }
}

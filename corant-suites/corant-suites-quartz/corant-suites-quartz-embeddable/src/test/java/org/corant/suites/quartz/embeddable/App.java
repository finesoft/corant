package org.corant.suites.quartz.embeddable;

import static org.corant.context.Instances.resolve;
import javax.enterprise.context.ApplicationScoped;
import org.corant.Corant;
import org.corant.shared.util.Threads;

@ApplicationScoped
public class App {

  public static void main(String[] args) {
    Corant.startup(TestJobService.class, App.class);
    Threads.tryThreadSleep(10000L);
    resolve(CorantDeclarativeScheduler.class).suspend();
    Threads.tryThreadSleep(10000L);
    resolve(CorantDeclarativeScheduler.class).resume(null);
    Threads.tryThreadSleep(10000L);
    resolve(CorantDeclarativeScheduler.class).suspend();
    Threads.tryThreadSleep(10000L);
    resolve(CorantDeclarativeScheduler.class).resume(null);
    Threads.tryThreadSleep(10000L);
    Corant.shutdown();
  }
}

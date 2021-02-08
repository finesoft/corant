package org.corant.suites.quartz.embeddable;

import org.corant.Corant;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class App {

  public static void main(String[] args) {
    Corant.startup(TestJobService.class,App.class);
  }
}

/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.quartz.embeddable;

import java.util.concurrent.TimeUnit;

/**
 * corant-modules-quartz-embeddable
 *
 * @author bingo 下午2:29:05
 */
public class Schedule {

  Long delay;

  TimeUnit unit;

  Long initialDelay;

  Long period;

  public Schedule(Long delay, TimeUnit unit, Long initialDelay, Long period) {
    this.delay = delay;
    this.unit = unit;
    this.initialDelay = initialDelay;
    this.period = period;
  }

  protected Schedule() {

  }

  public Long getDelay() {
    return delay;
  }

  public Long getInitialDelay() {
    return initialDelay;
  }

  public Long getPeriod() {
    return period;
  }

  public TimeUnit getUnit() {
    return unit;
  }

}

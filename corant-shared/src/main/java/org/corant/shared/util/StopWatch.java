/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.util;

import static org.corant.shared.util.StringUtils.EMPTY;
import static org.corant.shared.util.StringUtils.defaultString;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * corant-shared
 *
 * @author bingo 下午7:33:33
 *
 */
public class StopWatch {

  private final String id;

  private final List<TaskInfo> taskInfos = new LinkedList<>();

  private long startTimeMillis;

  private long totalTimeMillis;

  private String currentTaskName;

  private int taskCount;

  private TaskInfo lastTaskInfo;

  public StopWatch() {
    this(EMPTY);
  }

  public StopWatch(String id) {
    this.id = id;
  }

  public static StopWatch press(String taskName) {
    return new StopWatch().start(taskName);
  }

  public static StopWatch press(String id, String taskName) {
    return new StopWatch(id).start(taskName);
  }

  public void destroy() {
    destroy(null);
  }

  public void destroy(Consumer<StopWatch> consumer) {
    if (currentTaskName != null) {
      stop();
    }
    if (consumer != null) {
      consumer.accept(this);
    }
    taskInfos.clear();
    startTimeMillis = 0;
    totalTimeMillis = 0;
    currentTaskName = null;
    lastTaskInfo = null;
    taskCount = 0;
  }

  public String getCurrentTaskName() {
    return currentTaskName;
  }

  public String getId() {
    return id;
  }

  public TaskInfo getLastTaskInfo() {
    return lastTaskInfo;
  }

  public long getStartTimeMillis() {
    return startTimeMillis;
  }

  public int getTaskCount() {
    return taskCount;
  }

  public List<TaskInfo> getTaskInfos() {
    return Collections.unmodifiableList(taskInfos);
  }

  public long getTotalTimeMillis() {
    return totalTimeMillis;
  }

  public double getTotalTimeSeconds() {
    return totalTimeMillis / 1000.0;
  }

  public StopWatch start() {
    return start(EMPTY);
  }

  public StopWatch start(String taskName) {
    startTimeMillis = System.currentTimeMillis();
    currentTaskName = defaultString(taskName);
    return this;
  }

  public StopWatch stop() throws IllegalStateException {
    return stop(null);
  }

  public StopWatch stop(Consumer<TaskInfo> consumer) throws IllegalStateException {
    if (currentTaskName == null) {
      throw new IllegalStateException("Can't stop StopWatch: it's not running");
    }
    long lastTime = System.currentTimeMillis() - startTimeMillis;
    totalTimeMillis += lastTime;
    lastTaskInfo = new TaskInfo(currentTaskName, lastTime);
    if (consumer != null) {
      consumer.accept(lastTaskInfo);
    }
    ++taskCount;
    currentTaskName = null;
    return this;
  }

  public static final class TaskInfo {

    private final String taskName;

    private final long timeMillis;

    TaskInfo(String taskName, long timeMillis) {
      this.taskName = taskName;
      this.timeMillis = timeMillis;
    }

    public String getTaskName() {
      return taskName;
    }

    public long getTimeMillis() {
      return timeMillis;
    }

    public double getTimeSeconds() {
      return timeMillis / 1000.0;
    }
  }

}

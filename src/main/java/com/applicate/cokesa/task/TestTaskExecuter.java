package com.applicate.cokesa.task;

import com.applicate.services.channelkart.models.Task;
import com.applicate.services.channelkart.taskexecutors.AbstractTaskExecutor;


public class TestTaskExecuter extends AbstractTaskExecutor {
  @Override
  public void runTask(Task task) {
    System.out.println("Running task with Attributes "+task.getAttributes());
  }

  @Override
  public String getTaskType() {
    return "testTask";
  }
}

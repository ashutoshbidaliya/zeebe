/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.webapp.graphql.entity;

import static io.zeebe.tasklist.util.CollectionUtil.map;

import io.zeebe.tasklist.entities.TaskEntity;
import io.zeebe.tasklist.entities.TaskState;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public final class TaskDTO {

  private String id;
  private String workflowInstanceId;
  /** Field is used to resolve task name. */
  private String flowNodeBpmnId;

  private String flowNodeInstanceId;
  /** Field is used to resolve workflow name. */
  private String workflowId;
  /** Fallback value for workflow name. */
  private String bpmnProcessId;

  private OffsetDateTime creationTime;
  private OffsetDateTime completionTime;
  /** Field is used to return user data. */
  private String assigneeUsername;

  private TaskState taskState;

  public String getId() {
    return id;
  }

  public TaskDTO setId(String id) {
    this.id = id;
    return this;
  }

  public String getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public TaskDTO setWorkflowInstanceId(final String workflowInstanceId) {
    this.workflowInstanceId = workflowInstanceId;
    return this;
  }

  public String getFlowNodeBpmnId() {
    return flowNodeBpmnId;
  }

  public TaskDTO setFlowNodeBpmnId(String flowNodeBpmnId) {
    this.flowNodeBpmnId = flowNodeBpmnId;
    return this;
  }

  public String getFlowNodeInstanceId() {
    return flowNodeInstanceId;
  }

  public TaskDTO setFlowNodeInstanceId(final String flowNodeInstanceId) {
    this.flowNodeInstanceId = flowNodeInstanceId;
    return this;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public TaskDTO setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public TaskDTO setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
    return this;
  }

  public OffsetDateTime getCreationTime() {
    return creationTime;
  }

  public TaskDTO setCreationTime(OffsetDateTime creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  public OffsetDateTime getCompletionTime() {
    return completionTime;
  }

  public TaskDTO setCompletionTime(OffsetDateTime completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  public String getAssigneeUsername() {
    return assigneeUsername;
  }

  public TaskDTO setAssigneeUsername(String assigneeUsername) {
    this.assigneeUsername = assigneeUsername;
    return this;
  }

  public TaskState getTaskState() {
    return taskState;
  }

  public TaskDTO setTaskState(TaskState taskState) {
    this.taskState = taskState;
    return this;
  }

  public static TaskDTO createFrom(TaskEntity taskEntity) {
    return new TaskDTO()
        .setCompletionTime(taskEntity.getCompletionTime())
        .setCreationTime(taskEntity.getCreationTime())
        .setId(taskEntity.getId())
        .setWorkflowInstanceId(taskEntity.getWorkflowInstanceId())
        .setTaskState(taskEntity.getState())
        .setAssigneeUsername(taskEntity.getAssignee())
        .setBpmnProcessId(taskEntity.getBpmnProcessId())
        .setWorkflowId(taskEntity.getWorkflowId())
        .setFlowNodeBpmnId(taskEntity.getFlowNodeBpmnId())
        .setFlowNodeInstanceId(taskEntity.getFlowNodeInstanceId());
  }

  public static List<TaskDTO> createFrom(List<TaskEntity> taskEntities) {
    return map(taskEntities, t -> createFrom(t));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TaskDTO taskDTO = (TaskDTO) o;
    return Objects.equals(id, taskDTO.id)
        && Objects.equals(workflowInstanceId, taskDTO.workflowInstanceId)
        && Objects.equals(flowNodeBpmnId, taskDTO.flowNodeBpmnId)
        && Objects.equals(flowNodeInstanceId, taskDTO.flowNodeInstanceId)
        && Objects.equals(workflowId, taskDTO.workflowId)
        && Objects.equals(bpmnProcessId, taskDTO.bpmnProcessId)
        && Objects.equals(creationTime, taskDTO.creationTime)
        && Objects.equals(completionTime, taskDTO.completionTime)
        && Objects.equals(assigneeUsername, taskDTO.assigneeUsername)
        && taskState == taskDTO.taskState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        workflowInstanceId,
        flowNodeBpmnId,
        flowNodeInstanceId,
        workflowId,
        bpmnProcessId,
        creationTime,
        completionTime,
        assigneeUsername,
        taskState);
  }

  @Override
  public String toString() {
    return "TaskDTO{"
        + "id='"
        + id
        + '\''
        + ", workflowInstanceId='"
        + workflowInstanceId
        + '\''
        + ", flowNodeBpmnId='"
        + flowNodeBpmnId
        + '\''
        + ", workflowId='"
        + workflowId
        + '\''
        + ", bpmnProcessId='"
        + bpmnProcessId
        + '\''
        + ", creationTime="
        + creationTime
        + ", completionTime="
        + completionTime
        + ", assigneeUsername='"
        + assigneeUsername
        + '\''
        + ", taskState="
        + taskState
        + '}';
  }
}

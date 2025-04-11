/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.client;

import static io.camunda.client.api.search.enums.ProcessInstanceState.ACTIVE;
import static io.camunda.it.util.TestHelper.waitUntilJobWorkerHasFailedJob;
import static io.camunda.qa.util.multidb.CamundaMultiDBExtension.TIMEOUT_DATA_AVAILABILITY;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.enums.FlowNodeInstanceState;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.filter.ProcessInstanceFilter;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.statistics.response.ProcessElementStatistics;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.client.impl.statistics.response.ProcessFlowNodeStatisticsImpl;
import io.camunda.it.util.TestHelper;
import io.camunda.qa.util.multidb.MultiDbTest;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

@MultiDbTest
public class ProcessDefinitionStatisticsTest {

  public static final int INCIDENT_ERROR_HASH_CODE_V2 = 17551445;
  private static CamundaClient camundaClient;

  private static void waitForProcessInstances(
      final int count, final Consumer<ProcessInstanceFilter> fn) {
    Awaitility.await("should receive data from ES")
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () ->
                assertThat(
                        camundaClient
                            .newProcessInstanceSearchRequest()
                            .filter(fn)
                            .send()
                            .join()
                            .items())
                    .hasSize(count));
  }

  private static void waitForUserTasks(final int count, final long processDefinitionKey) {
    Awaitility.await("should receive data from ES")
        .atMost(TIMEOUT_DATA_AVAILABILITY)
        .ignoreExceptions() // Ignore exceptions and continue retrying
        .untilAsserted(
            () ->
                assertThat(
                        camundaClient
                            .newUserTaskSearchRequest()
                            .filter(f -> f.processDefinitionKey(processDefinitionKey))
                            .send()
                            .join()
                            .items())
                    .hasSize(count));
  }

  @Test
  void shouldGetEmptyStatisticsWithoutMatch() {
    // when
    final var actual = camundaClient.newProcessDefinitionElementStatisticsRequest(1L).send().join();

    // then
    assertThat(actual).hasSize(0);
  }

  @Test
  void shouldGetStatisticsAndFilterByProcessInstanceKey() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    final var pi1 = createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(
        2, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.processInstanceKey(pi1.getProcessInstanceKey()))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsAndFilterByProcessInstanceKeyIn() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    final var pi1 = createInstance(processDefinitionKey);
    final var pi2 = createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(
        3, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(
                f ->
                    f.processInstanceKey(
                        b -> b.in(pi1.getProcessInstanceKey(), pi2.getProcessInstanceKey())))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 2L));
  }

  @Test
  void shouldGetStatisticsAndFilterByProcessInstanceKeyNotIn() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    final var pi1 = createInstance(processDefinitionKey);
    final var pi2 = createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(
        3, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
            .filter(
                f ->
                    f.processInstanceKey(
                        b -> b.notIn(pi1.getProcessInstanceKey(), pi2.getProcessInstanceKey())))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsAndFilterByTenantIdLike() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(
        2, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.tenantId(b -> b.like("*def*")))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 2L));
  }

  @Test
  void shouldGetStatisticsAndFilterByStartDateFilterGtLt() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    createInstance(processDefinitionKey);
    final var piKey = createInstance(processDefinitionKey).getProcessInstanceKey();
    waitForProcessInstances(
        2, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));
    final var pi = getProcessInstance(piKey);
    final var startDate = OffsetDateTime.parse(pi.getStartDate());

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(
                f ->
                    f.startDate(
                        b ->
                            b.gt(startDate.minus(1, ChronoUnit.MILLIS))
                                .lt(startDate.plus(1, ChronoUnit.MILLIS))))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsAndFilterByStartDateFilterGteLte() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    createInstance(processDefinitionKey);
    final var piKey = createInstance(processDefinitionKey).getProcessInstanceKey();
    waitForProcessInstances(
        2, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));
    final var pi = getProcessInstance(piKey);
    final var startDate = OffsetDateTime.parse(pi.getStartDate());

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.startDate(b -> b.gte(startDate).lte(startDate)))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsAndFilterByEndDateExists() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(2, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.endDate(b -> b.exists(true)))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsAndFilterByEndDateNotExists() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(2, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.endDate(b -> b.exists(false)))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 1L, 0L, 0L, 0L));
  }

  @Test
  void shouldGetStatisticsAndFilterByStateActive() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(3, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(3, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.state(ACTIVE))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 2L, 0L, 0L, 0L));
  }

  @Test
  void shouldGetDistinctStatisticsForMultiInstanceActivity() {
    // given
    final var processModel =
        Bpmn.createExecutableProcess("process")
            .startEvent("StartEvent")
            .userTask("UserTaskMultiInstance")
            .zeebeUserTask()
            .multiInstance()
            .parallel()
            .zeebeInputCollectionExpression("[1,2,3]")
            .multiInstanceDone()
            .endEvent("EndEvent")
            .done();
    final var processDefinitionKey =
        deployResource(processModel, "multi-instance.bpmn")
            .getProcesses()
            .getFirst()
            .getProcessDefinitionKey();

    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(6, processDefinitionKey);

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.state(ACTIVE))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("UserTaskMultiInstance", 2L, 0L, 0L, 0L));
  }

  @Test
  void shouldGetStatisticsAndFilterByStateNotEq() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(2, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.state(b -> b.neq(ACTIVE)))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldGetStatisticsForCompleted() {
    // given
    final var processDefinitionKey = deployCompleteBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(
        3, f -> f.processDefinitionKey(processDefinitionKey).state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 3L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 3L));
  }

  @Test
  void shouldGetStatisticsForActive() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 2L, 0L, 0L, 0L));
  }

  @Test
  void shouldGetStatisticsForIncidentsAndFilterByHasIncident() {
    // given
    final var processDefinitionKey = deployIncidentBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(3, f -> f.processDefinitionKey(processDefinitionKey).hasIncident(true));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .filter(f -> f.hasIncident(true))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 3L),
            new ProcessFlowNodeStatisticsImpl("ScriptTask", 0L, 0L, 3L, 0L));
  }

  @Test
  void shouldReturnStatisticsForCanceled() {
    // given
    final var processModel =
        Bpmn.createExecutableProcess("process")
            .startEvent("StartEvent")
            .userTask("UserTask")
            .endEvent()
            .done();
    final var processDefinitionKey =
        deployResource(processModel, "manual_task_cancel.bpmn")
            .getProcesses()
            .getFirst()
            .getProcessDefinitionKey();
    final var pi1 = createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    camundaClient.newCancelInstanceCommand(pi1.getProcessInstanceKey()).send().join();
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionElementStatisticsRequest(processDefinitionKey)
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 1L, 1L, 0L, 0L));
  }

  @Test
  void shouldReturnStatisticsAndFilterByHasRetriesLeft() {
    // given
    final var processDefinitionKey =
        TestHelper.deployResource(camundaClient, "process/service_tasks_v1.bpmn")
            .getProcesses()
            .getFirst()
            .getProcessDefinitionKey();
    TestHelper.startProcessInstance(camundaClient, "service_tasks_v1", "{\"xyz\":\"foo\"}");

    try (final JobWorker ignored =
        camundaClient
            .newWorker()
            .jobType("taskA")
            .handler((client, job) -> client.newFailCommand(job).retries(1).send().join())
            .open()) {

      waitUntilJobWorkerHasFailedJob(camundaClient);

      // when
      final var actual =
          camundaClient
              .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
              .filter(f -> f.hasRetriesLeft(true))
              .send()
              .join();

      // then
      assertThat(actual).hasSize(2);
      assertThat(actual)
          .containsExactlyInAnyOrder(
              new ProcessFlowNodeStatisticsImpl("start", 0L, 0L, 0L, 1L),
              new ProcessFlowNodeStatisticsImpl("taskA", 1L, 0L, 0L, 0L));
    }
  }

  @Test
  void shouldReturnStatisticsAndFilterByFlowNodeId() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(2, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
            .filter(f -> f.flowNodeId("UserTask"))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(1);
    assertThat(actual)
        .containsExactlyInAnyOrder(new ProcessFlowNodeStatisticsImpl("UserTask", 1L, 0L, 0L, 1L));
  }

  @Test
  void shouldReturnStatisticsAndFilterByFlowNodeInstanceState() {
    // given
    final var processDefinitionKey = deployActiveBPMN();
    createInstance(processDefinitionKey);
    createInstance(processDefinitionKey);
    waitForProcessInstances(2, f -> f.processDefinitionKey(processDefinitionKey).state(ACTIVE));
    waitForUserTasks(2, processDefinitionKey);
    final var userTask = getUserTask(processDefinitionKey);
    camundaClient.newUserTaskCompleteCommand(userTask.getUserTaskKey()).send().join();
    waitForProcessInstances(
        1,
        f ->
            f.processInstanceKey(userTask.getProcessInstanceKey())
                .state(ProcessInstanceState.COMPLETED));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
            .filter(f -> f.flowNodeInstanceState(FlowNodeInstanceState.COMPLETED))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(3);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("StartEvent", 0L, 0L, 0L, 2L),
            new ProcessFlowNodeStatisticsImpl("UserTask", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("EndEvent", 0L, 0L, 0L, 1L));
  }

  @Test
  void shouldReturnStatisticsAndFilterByFlowNodeInstanceIncident() {
    // given
    final var processDefinitionKey = deployIncidentBPMN();
    createInstance(processDefinitionKey);
    waitForProcessInstances(1, f -> f.processDefinitionKey(processDefinitionKey).hasIncident(true));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
            .filter(f -> f.hasFlowNodeInstanceIncident(true))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(1);
    assertThat(actual)
        .containsExactly(new ProcessFlowNodeStatisticsImpl("ScriptTask", 0L, 0L, 1L, 0L));
  }

  @Test
  void shouldReturnStatisticsAndFilterByErrorHashCode() {
    // given
    final var processDefinitionKey =
        TestHelper.deployResource(camundaClient, "process/incident_process_v2.bpmn")
            .getProcesses()
            .getFirst()
            .getProcessDefinitionKey();
    createInstance(processDefinitionKey);
    waitForProcessInstances(1, f -> f.processDefinitionKey(processDefinitionKey).hasIncident(true));

    // when
    final var actual =
        camundaClient
            .newProcessDefinitionFlowNodeStatisticsRequest(processDefinitionKey)
            .filter(f -> f.incidentErrorHashCode(INCIDENT_ERROR_HASH_CODE_V2))
            .send()
            .join();

    // then
    assertThat(actual).hasSize(2);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            new ProcessFlowNodeStatisticsImpl("start", 0L, 0L, 0L, 1L),
            new ProcessFlowNodeStatisticsImpl("taskAIncident", 0L, 0L, 1L, 0L));
  }

  private static DeploymentEvent deployResource(
      final BpmnModelInstance processModel, final String resourceName) {
    return camundaClient
        .newDeployResourceCommand()
        .addProcessModel(processModel, resourceName)
        .send()
        .join();
  }

  private static long deployCompleteBPMN() {
    final var processModel =
        Bpmn.createExecutableProcess("process")
            .startEvent("StartEvent")
            .endEvent("EndEvent")
            .done();
    return deployResource(processModel, "complete.bpmn")
        .getProcesses()
        .getFirst()
        .getProcessDefinitionKey();
  }

  private static long deployActiveBPMN() {
    final var processModel =
        Bpmn.createExecutableProcess("process")
            .startEvent("StartEvent")
            .userTask("UserTask")
            .zeebeUserTask()
            .endEvent("EndEvent")
            .done();
    return deployResource(processModel, "manual_task.bpmn")
        .getProcesses()
        .getFirst()
        .getProcessDefinitionKey();
  }

  private static long deployIncidentBPMN() {
    final var processModel =
        Bpmn.createExecutableProcess("process")
            .startEvent("StartEvent")
            .scriptTask(
                "ScriptTask",
                b -> b.zeebeExpression("assert(x, x != null)").zeebeResultVariable("res"))
            .zeebeResultVariable("res")
            .endEvent()
            .done();
    return deployResource(processModel, "script_task.bpmn")
        .getProcesses()
        .getFirst()
        .getProcessDefinitionKey();
  }

  private static ProcessInstanceEvent createInstance(final long processDefinitionKey) {
    return camundaClient
        .newCreateInstanceCommand()
        .processDefinitionKey(processDefinitionKey)
        .send()
        .join();
  }

  private static ProcessInstance getProcessInstance(final long piKey) {
    return camundaClient
        .newProcessInstanceSearchRequest()
        .filter(f -> f.processInstanceKey(piKey))
        .page(p -> p.limit(1))
        .send()
        .join()
        .items()
        .getFirst();
  }

  private static UserTask getUserTask(final long processDefinitionKey) {
    return camundaClient
        .newUserTaskSearchRequest()
        .filter(f -> f.processDefinitionKey(processDefinitionKey))
        .send()
        .join()
        .items()
        .getFirst();
  }
}

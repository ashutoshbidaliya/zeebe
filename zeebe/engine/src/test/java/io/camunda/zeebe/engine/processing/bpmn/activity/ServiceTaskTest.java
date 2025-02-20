/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.bpmn.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnJobBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnJobBehavior.LinkedResourceProps;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeBindingType;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.camunda.bpm.model.xml.impl.util.ReflectUtil;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class ServiceTaskTest {
  @ClassRule public static final EngineRule ENGINE = EngineRule.singlePartition();
  private static final String PROCESS_ID = "process";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  @Test
  public void shouldCreateJobWithLinkedResourcesVersionTagBinding() throws JsonProcessingException {
    final var rpaScript =
        """
          {
          "type": "default",
          "id": "Rpa_0w7r08e",
          "executionPlatform": "Camunda Cloud",
          "executionPlatformVersion": "8.1.0",
          "schemaVersion": 7,
          "versionTag": "1v",
          "resource": "Script content"
        }""";

    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("Rpa_0w7r08e")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.versionTag)
                                    .versionTag("1v")
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE
        .deployment()
        .withJsonResource(rpaScript.getBytes(StandardCharsets.UTF_8), "my_resource.rpa")
        .withXmlResource(modelInstance)
        .deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    final Record<JobRecordValue> jobCreated =
        RecordingExporter.jobRecords(JobIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst();

    assertThat(jobCreated.getValue().getCustomHeaders()).containsKey("linkedResources");
    final List<LinkedResourceProps> resourcePropsList =
        MAPPER.readValue(
            jobCreated.getValue().getCustomHeaders().get("linkedResources"),
            new TypeReference<>() {});

    assertThat(resourcePropsList).hasSize(1);
    final LinkedResourceProps resourceProps = resourcePropsList.get(0);
    assertThat(resourceProps)
        .hasFieldOrPropertyWithValue("resourceType", "RPA")
        .hasFieldOrPropertyWithValue("linkName", "my_link");
    assertThat(resourceProps.getResourceKey()).matches("\\d+");
  }

  @Test
  public void shouldCreateJobWithLinkedResourcesLatestBinding() throws JsonProcessingException {
    final var rpaScript =
        """
          {
          "type": "default",
          "id": "Rpa_0w7r08e",
          "executionPlatform": "Camunda Cloud",
          "executionPlatformVersion": "8.1.0",
          "schemaVersion": 7,
          "resource": "Script content"
        }""";

    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("Rpa_0w7r08e")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.latest)
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE
        .deployment()
        .withJsonResource(rpaScript.getBytes(StandardCharsets.UTF_8), "my_resource.rpa")
        .withXmlResource(modelInstance)
        .deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    final Record<JobRecordValue> jobCreated =
        RecordingExporter.jobRecords(JobIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst();

    assertThat(jobCreated.getValue().getCustomHeaders()).containsKey("linkedResources");
    final List<LinkedResourceProps> resourcePropsList =
        MAPPER.readValue(
            jobCreated.getValue().getCustomHeaders().get("linkedResources"),
            new TypeReference<>() {});

    assertThat(resourcePropsList).hasSize(1);
    final LinkedResourceProps resourceProps = resourcePropsList.get(0);
    assertThat(resourceProps)
        .hasFieldOrPropertyWithValue("resourceType", "RPA")
        .hasFieldOrPropertyWithValue("linkName", "my_link");
    assertThat(resourceProps.getResourceKey()).matches("\\d+");
  }

  @Test
  public void shouldCreateJobWithLinkedResourcesDeploymentBinding() throws JsonProcessingException {
    final var rpaScript =
        """
          {
          "type": "default",
          "id": "Rpa_0w7r08e",
          "executionPlatform": "Camunda Cloud",
          "executionPlatformVersion": "8.1.0",
          "schemaVersion": 7,
          "resource": "Script content"
        }""";

    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("Rpa_0w7r08e")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.deployment)
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE
        .deployment()
        .withJsonResource(rpaScript.getBytes(StandardCharsets.UTF_8), "my_resource.rpa")
        .withXmlResource(modelInstance)
        .deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    final Record<JobRecordValue> jobCreated =
        RecordingExporter.jobRecords(JobIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst();

    assertThat(jobCreated.getValue().getCustomHeaders()).containsKey("linkedResources");
    final List<LinkedResourceProps> resourcePropsList =
        MAPPER.readValue(
            jobCreated.getValue().getCustomHeaders().get("linkedResources"),
            new TypeReference<>() {});

    assertThat(resourcePropsList).hasSize(1);
    final LinkedResourceProps resourceProps = resourcePropsList.get(0);
    assertThat(resourceProps)
        .hasFieldOrPropertyWithValue("resourceType", "RPA")
        .hasFieldOrPropertyWithValue("linkName", "my_link");
    assertThat(resourceProps.getResourceKey()).matches("\\d+");
  }

  @Test
  public void shouldHandleNotFoundVersionTagBinding() {
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("2")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.versionTag)
                                    .versionTag("1v")
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE.deployment().withXmlResource(modelInstance).deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    assertThat(
            RecordingExporter.incidentRecords(IncidentIntent.CREATED)
                .withProcessInstanceKey(processInstanceKey)
                .getFirst()
                .getValue())
        .extracting(IncidentRecordValue::getErrorType, IncidentRecordValue::getErrorMessage)
        .describedAs("Expect that incident is raised when linked resource is not found")
        .containsExactly(
            ErrorType.RESOURCE_NOT_FOUND,
            String.format(
                BpmnJobBehavior.FIND_RESOURCE_BY_ID_AND_VERSION_TAG_FAILED_MESSAGE, "2", "1v"));
  }

  @Test
  public void shouldHandleNotFoundDeploymentBinding() {
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("2")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.deployment)
                                    .versionTag("1v")
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE.deployment().withXmlResource(modelInstance).deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    final IncidentRecordValue incidentRecordValue =
        RecordingExporter.incidentRecords(IncidentIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst()
            .getValue();
    assertThat(incidentRecordValue.getErrorType()).isEqualTo(ErrorType.RESOURCE_NOT_FOUND);
    assertThat(incidentRecordValue.getErrorMessage())
        .matches(
            String.format(
                BpmnJobBehavior.FIND_RESOURCE_BY_ID_IN_SAME_DEPLOYMENT_FAILED_MESSAGE,
                "2",
                "\\d+"));
  }

  @Test
  public void shouldHandleNotFoundLatestBinding() {
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("2")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.latest)
                                    .versionTag("1v")
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE.deployment().withXmlResource(modelInstance).deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    assertThat(
            RecordingExporter.incidentRecords(IncidentIntent.CREATED)
                .withProcessInstanceKey(processInstanceKey)
                .getFirst()
                .getValue())
        .extracting(IncidentRecordValue::getErrorType, IncidentRecordValue::getErrorMessage)
        .describedAs("Expect that incident is raised when linked resource is not found")
        .containsExactly(
            ErrorType.RESOURCE_NOT_FOUND,
            String.format(BpmnJobBehavior.FIND_LATEST_RESOURCE_BY_ID_FAILED_MESSAGE, "2"));
  }

  @Test
  public void shouldResolveIncidentAfterNotFoundResource() {
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask(
                "my_linked_resource",
                t ->
                    t.zeebeLinkedResources(
                            l ->
                                l.resourceId("2")
                                    .resourceType("RPA")
                                    .bindingType(ZeebeBindingType.latest)
                                    .versionTag("1v")
                                    .linkName("my_link"))
                        .zeebeJobType("type"))
            .endEvent()
            .done();

    ENGINE.deployment().withXmlResource(modelInstance).deploy();

    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // assume
    final var incident =
        RecordingExporter.incidentRecords(IncidentIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst();
    assertThat(incident).isNotNull();
    // when
    // deploy missing rpa
    final var rpaScript =
        """
          {
          "type": "default",
          "id": "Rpa_0w7r08e",
          "executionPlatform": "Camunda Cloud",
          "executionPlatformVersion": "8.1.0",
          "schemaVersion": 7,
          "resource": "Script content"
        }""";
    ENGINE
        .deployment()
        .withJsonResource(rpaScript.getBytes(StandardCharsets.UTF_8), "my_resource.rpa")
        .deploy();
    // resolve incident
    ENGINE.incident().ofInstance(processInstanceKey).withKey(incident.getKey()).resolve();
    // then
    assertThat(RecordingExporter.incidentRecords().onlyEvents().limit(2))
        .extracting(io.camunda.zeebe.protocol.record.Record::getKey, Record::getIntent)
        .describedAs("resource not found incident is resolved and no new incident is created")
        .containsExactly(
            tuple(incident.getKey(), IncidentIntent.CREATED),
            tuple(incident.getKey(), IncidentIntent.RESOLVED));
  }

  @Test
  public void shouldNotIncludeLinkedResourcesWhenTheyAreNotInServiceTask() {
    final BpmnModelInstance modelInstance =
        Bpmn.readModelFromStream(
            ReflectUtil.getResourceAsStream("resource/test-send-task-with-linked-resources.bpmn"));

    ENGINE.deployment().withXmlResource(modelInstance).deploy();

    // when
    final long processInstanceKey = ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create();

    // then
    final Record<JobRecordValue> jobCreated =
        RecordingExporter.jobRecords(JobIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .getFirst();

    assertThat(jobCreated.getValue().getCustomHeaders()).doesNotContainKey("linkedResources");
  }
}

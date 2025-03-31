/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.migration;

import static io.camunda.it.migration.util.PrefixMigrationITUtils.GATEWAY_GRPC_PORT;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.MANAGEMENT_PORT;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.OLD_OPERATE_PREFIX;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.OLD_TASKLIST_PREFIX;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.SERVER_PORT;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.createCamundaClient;
import static io.camunda.it.migration.util.PrefixMigrationITUtils.requestProcessInstanceFromV1;
import static io.camunda.qa.util.multidb.CamundaMultiDBExtension.currentMultiDbDatabaseType;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerCmd;
import io.camunda.application.commons.search.SearchEngineDatabaseConfiguration.SearchEngineConnectProperties;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.exporter.adapters.ClientAdapter;
import io.camunda.exporter.schema.SchemaManager;
import io.camunda.exporter.schema.config.SearchEngineConfiguration;
import io.camunda.operate.property.OperateProperties;
import io.camunda.qa.util.cluster.TestSimpleCamundaApplication;
import io.camunda.qa.util.multidb.CamundaMultiDBExtension.DatabaseType;
import io.camunda.qa.util.multidb.ElasticOpenSearchSetupHelper;
import io.camunda.qa.util.multidb.MultiDbTest;
import io.camunda.qa.util.multidb.MultiDbTestApplication;
import io.camunda.search.connect.configuration.ConnectConfiguration;
import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.webapps.schema.descriptors.IndexDescriptors;
import io.camunda.zeebe.qa.util.cluster.TestPrefixMigrationApp;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

@MultiDbTest
@DisabledIfSystemProperty(named = "test.integration.camunda.database.type", matches = "rdbms")
@DisabledIfSystemProperty(named = "test.integration.camunda.database.type", matches = "AWS_OS")
public class PrefixMigrationIT {
  private static final String DEFAULT_ES_OS_URL_FOR_MULTI_DB =
      "http://host.testcontainers.internal:9200";
  private static final int TOTAL_NUMBER_OPERATE_TASKLIST_INDICES_BEFORE_HARMONISATION = 34;

  @MultiDbTestApplication(managedLifecycle = false)
  private static final TestSimpleCamundaApplication STANDALONE_CAMUNDA =
      new TestSimpleCamundaApplication().withBasicAuth().withAuthorizationsEnabled();

  @BeforeAll
  public static void beforeAll() {
    // The container from createCamundaContainer needs access to the ES/OS instances on the host
    // machine
    Testcontainers.exposeHostPorts(9200); // elasticsearch
  }

  private GenericContainer<?> createCamundaContainer() {
    final var container =
        new GenericContainer<>(DockerImageName.parse("camunda/camunda:8.7.0-SNAPSHOT"))
            .waitingFor(
                new HttpWaitStrategy()
                    .forPort(MANAGEMENT_PORT)
                    .forPath("/actuator/health")
                    .withReadTimeout(Duration.ofSeconds(30)))
            .withAccessToHost(true)
            .withExposedPorts(SERVER_PORT, MANAGEMENT_PORT, GATEWAY_GRPC_PORT)
            .withEnv("CAMUNDA_OPERATE_CSRFPREVENTIONENABLED", "false")
            .withEnv("CAMUNDA_TASKLIST_CSRFPREVENTIONENABLED", "false")
            .withEnv("SPRING_PROFILES_ACTIVE", "broker,operate,tasklist,identity,consolidated-auth")
            .withEnv("CAMUNDA_SECURITY_AUTHENTICATION_METHOD", "BASIC");

    if (currentMultiDbDatabaseType() == DatabaseType.ES) {
      addElasticsearchProperties(container, OLD_OPERATE_PREFIX, OLD_TASKLIST_PREFIX);
    } else if (currentMultiDbDatabaseType() == DatabaseType.OS) {
      addOpensearchProperties(container, OLD_OPERATE_PREFIX, OLD_TASKLIST_PREFIX);
    }

    return container;
  }

  private void addOpensearchProperties(
      final GenericContainer<?> container,
      final String operatePrefix,
      final String tasklistPrefix) {
    container
        .withEnv("CAMUNDA_DATABASE_TYPE", "opensearch")
        .withEnv("CAMUNDA_OPERATE_DATABASE", "opensearch")
        .withEnv("CAMUNDA_TASKLIST_DATABASE", "opensearch")
        .withEnv("CAMUNDA_DATABASE_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_OPERATE_OPENSEARCH_INDEX_PREFIX", operatePrefix)
        .withEnv("CAMUNDA_TASKLIST_OPENSEARCH_INDEX_PREFIX", tasklistPrefix)
        .withEnv("CAMUNDA_TASKLIST_OPENSEARCH_INDEXPREFIX", tasklistPrefix)
        .withEnv(
            "ZEEBE_BROKER_EXPORTERS_OPENSEARCH_CLASSNAME",
            "io.camunda.zeebe.exporter.opensearch.OpensearchExporter")
        .withEnv("CAMUNDA_OPERATE_OPENSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_OPERATE_ZEEBEOPENSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_TASKLIST_OPENSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_TASKLIST_ZEEBEOPENSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("ZEEBE_BROKER_EXPORTERS_OPENSEARCH_ARGS_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB);
  }

  private void addElasticsearchProperties(
      final GenericContainer<?> container,
      final String operatePrefix,
      final String tasklistPrefix) {
    container
        .withEnv("CAMUNDA_OPERATE_ELASTICSEARCH_INDEX_PREFIX", operatePrefix)
        .withEnv("CAMUNDA_TASKLIST_ELASTICSEARCH_INDEX_PREFIX", tasklistPrefix)
        .withEnv(
            "ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_CLASSNAME",
            "io.camunda.zeebe.exporter.ElasticsearchExporter")
        .withEnv("CAMUNDA_DATABASE_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_OPERATE_ELASTICSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_OPERATE_ZEEBEELASTICSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_TASKLIST_ELASTICSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("CAMUNDA_TASKLIST_ZEEBEELASTICSEARCH_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB)
        .withEnv("ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_URL", DEFAULT_ES_OS_URL_FOR_MULTI_DB);
  }

  private void prefixMigration(
      final String oldOperatePrefix, final String oldTasklistPrefix, final String newPrefix) {
    final var operate = new OperateProperties();
    final var tasklist = new TasklistProperties();
    final var connect = new SearchEngineConnectProperties();

    operate.getElasticsearch().setIndexPrefix(oldOperatePrefix);
    operate.getOpensearch().setIndexPrefix(oldOperatePrefix);

    tasklist.getElasticsearch().setIndexPrefix(oldTasklistPrefix);
    tasklist.getOpenSearch().setIndexPrefix(oldTasklistPrefix);

    connect.setIndexPrefix(newPrefix);
    if (currentMultiDbDatabaseType() == DatabaseType.ES) {
      connect.setType("elasticsearch");
    } else if (currentMultiDbDatabaseType() == DatabaseType.OS) {
      connect.setType("opensearch");
    }

    try (final var app = new TestPrefixMigrationApp(connect, tasklist, operate)) {
      app.start();
    }
  }

  @Test
  void shouldMigrateCorrectIndicesDuringPrefixMigration() {
    // given
    final var tasklistContainer =
        new GenericContainer<>("camunda/zeebe:8.7.0-SNAPSHOT")
            .withCreateContainerCmdModifier(
                (final CreateContainerCmd cmd) ->
                    cmd.withEntrypoint("/usr/local/zeebe/bin/tasklist"))
            .withAccessToHost(true);

    final var operateContainer =
        new GenericContainer<>("camunda/zeebe:8.7.0-SNAPSHOT")
            .withCreateContainerCmdModifier(
                (final CreateContainerCmd cmd) ->
                    cmd.withEntrypoint("/usr/local/zeebe/bin/operate"))
            .withAccessToHost(true);

    // to avoid collisions with other tests
    final var shortUUID = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
    final var oldOperatePrefix = shortUUID + "-old-operate-prefix";
    final var oldTasklistPrefix = shortUUID + "-old-tasklist-prefix";
    final var newPrefix = shortUUID + "-new-prefix";

    if (currentMultiDbDatabaseType() == DatabaseType.ES) {
      addElasticsearchProperties(tasklistContainer, oldOperatePrefix, oldTasklistPrefix);
      addElasticsearchProperties(operateContainer, oldOperatePrefix, newPrefix);
    } else if (currentMultiDbDatabaseType() == DatabaseType.OS
        || currentMultiDbDatabaseType() == DatabaseType.AWS_OS) {
      addOpensearchProperties(tasklistContainer, oldOperatePrefix, oldTasklistPrefix);
      addOpensearchProperties(operateContainer, oldOperatePrefix, newPrefix);
    }

    // creates the 8.7 operate/tasklist indices
    tasklistContainer.start();
    operateContainer.start();

    final var setupHelper =
        new ElasticOpenSearchSetupHelper(
            "http://localhost:9200",
            Collections.nCopies(TOTAL_NUMBER_OPERATE_TASKLIST_INDICES_BEFORE_HARMONISATION, null));

    // validate all 8.7 operate/tasklist indices have been created
    Awaitility.await("Await schema readiness")
        .timeout(Duration.ofMinutes(1))
        .pollInterval(Duration.ofMillis(500))
        .until(() -> setupHelper.validateSchemaCreation(shortUUID));

    operateContainer.stop();
    tasklistContainer.stop();

    // when
    prefixMigration(oldOperatePrefix, oldTasklistPrefix, newPrefix);

    // then
    final var connectConfig = new ConnectConfiguration();
    if (currentMultiDbDatabaseType() == DatabaseType.OS
        || currentMultiDbDatabaseType() == DatabaseType.AWS_OS) {
      connectConfig.setType("opensearch");
    }

    final var searchEngineClient = ClientAdapter.of(connectConfig).getSearchEngineClient();
    final var expectedDescriptors =
        new IndexDescriptors(newPrefix, currentMultiDbDatabaseType() == DatabaseType.ES);

    final var schemaManager =
        new SchemaManager(
            searchEngineClient,
            expectedDescriptors.indices(),
            expectedDescriptors.templates(),
            SearchEngineConfiguration.of(b -> b),
            new ObjectMapper());

    Awaitility.await("All indices migrated")
        .untilAsserted(
            () -> {
              Assertions.assertThat(schemaManager.isSchemaReadyForUse()).isTrue();
            });

    setupHelper.cleanup(shortUUID);
    setupHelper.close();
  }

  @Test
  void shouldReindexDocumentsDuringPrefixMigration() throws IOException {
    // given
    final var camunda87 = createCamundaContainer();
    camunda87.start();

    final var camunda87Client = createCamundaClient(camunda87);

    final var event =
        camunda87Client
            .newDeployResourceCommand()
            .addResourceFromClasspath("process/incident_process_v1.bpmn")
            .send()
            .join();

    final long processDefinitionKey = event.getProcesses().getFirst().getProcessDefinitionKey();
    final ProcessInstanceEvent processInstanceEvent =
        camunda87Client
            .newCreateInstanceCommand()
            .processDefinitionKey(processDefinitionKey)
            .send()
            .join();

    // Wait for documents to be written to indices
    Awaitility.await("document should be written")
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              final long processInstanceKey = processInstanceEvent.getProcessInstanceKey();
              final HttpResponse<String> processInstanceResponse =
                  requestProcessInstanceFromV1(
                      String.format("http://localhost:%d/", camunda87.getMappedPort(SERVER_PORT)),
                      processInstanceKey);

              assertThat(processInstanceResponse.statusCode()).isEqualTo(200);
              assertThat(processInstanceResponse.body())
                  .contains(Long.toString(processInstanceKey));
            });

    camunda87.stop();

    // when
    prefixMigration(OLD_OPERATE_PREFIX, OLD_TASKLIST_PREFIX, "prefixmigrationit");

    // then
    STANDALONE_CAMUNDA.start();
    STANDALONE_CAMUNDA.awaitCompleteTopology();
    try (final var currentCamundaClient = STANDALONE_CAMUNDA.newClientBuilder().build()) {
      final var processDefinitions =
          currentCamundaClient.newProcessDefinitionSearchRequest().send().join();
      assertThat(processDefinitions.items().size()).isEqualTo(1);
      assertThat(processDefinitions.items().getFirst().getProcessDefinitionKey())
          .isEqualTo(event.getProcesses().getFirst().getProcessDefinitionKey());
    }

    STANDALONE_CAMUNDA.stop();
  }
}

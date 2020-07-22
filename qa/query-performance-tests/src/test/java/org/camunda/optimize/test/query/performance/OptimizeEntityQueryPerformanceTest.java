/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.test.query.performance;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.IdentityType;
import org.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.RoleType;
import org.camunda.optimize.dto.optimize.TenantDto;
import org.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionDataDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.dashboard.ReportLocationDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportItemDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.service.es.schema.index.AlertIndex;
import org.camunda.optimize.service.es.schema.index.CollectionIndex;
import org.camunda.optimize.service.es.schema.index.DashboardIndex;
import org.camunda.optimize.service.es.schema.index.DecisionDefinitionIndex;
import org.camunda.optimize.service.es.schema.index.ProcessDefinitionIndex;
import org.camunda.optimize.service.es.schema.index.report.CombinedReportIndex;
import org.camunda.optimize.service.es.schema.index.report.SingleDecisionReportIndex;
import org.camunda.optimize.service.es.schema.index.report.SingleProcessReportIndex;
import org.camunda.optimize.service.util.IdGenerator;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.camunda.optimize.test.util.TemplatedProcessReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.TENANT_INDEX_NAME;

@Slf4j
public class OptimizeEntityQueryPerformanceTest extends AbstractQueryPerformanceTest {

  private static final String DEFAULT_USER = "demo";

  private static ProcessDefinitionOptimizeDto processDefinitionOptimizeDto;
  private static DecisionDefinitionOptimizeDto decisionDefinitionOptimizeDto;

  @BeforeEach
  public void init() {
    addTenantsToElasticsearch();
    processDefinitionOptimizeDto = addProcessDefinitionToOptimize();
    decisionDefinitionOptimizeDto = addDecisionDefinitionToOptimize();
  }

  @Test
  public void testQueryPerformance_getProcessReports() {
    // given
    final int numberOfReports = getNumberOfEntities();
    addSingleProcessReportsToOptimize(numberOfReports);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<ReportDefinitionDto> reportDefinitionDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllPrivateReportsRequest()
      .executeAndReturnList(ReportDefinitionDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(reportDefinitionDtos).hasSize(numberOfReports);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  @Test
  public void testQueryPerformance_getDecisionReports() {
    // given
    final int numberOfReports = getNumberOfEntities();
    addSingleDecisionReportsToOptimize(numberOfReports);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<ReportDefinitionDto> reportDefinitionDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllPrivateReportsRequest()
      .executeAndReturnList(ReportDefinitionDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(reportDefinitionDtos).hasSize(numberOfReports);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  @Test
  public void testQueryPerformance_getCombinedReports() {
    // given
    final int numberOfReports = getNumberOfEntities();
    addCombinedReportsToOptimize(numberOfReports);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<ReportDefinitionDto> reportDefinitionDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllPrivateReportsRequest()
      .executeAndReturnList(ReportDefinitionDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(reportDefinitionDtos).hasSize(numberOfReports);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  @Test
  public void testQueryPerformance_getAllEntities() {
    // given
    final int totalNumberOfEntities = getNumberOfEntities();
    // We consider 5 entity types: process reports / decision reports / combined reports / dashboards/ collections
    final int numOfEntityTypes = 5;
    final int numOfEachEntityToAdd = totalNumberOfEntities / numOfEntityTypes;
    // if the overall number is not divisible by the number of entity type, we add some extra dashboards
    final int extraDashboardsToAdd = totalNumberOfEntities % numOfEntityTypes;

    addSingleProcessReportsToOptimize(numOfEachEntityToAdd);
    addSingleDecisionReportsToOptimize(numOfEachEntityToAdd);
    addCombinedReportsToOptimize(numOfEachEntityToAdd);
    addDashboardsToOptimize(numOfEachEntityToAdd + extraDashboardsToAdd);
    addCollectionsToOptimize(numOfEachEntityToAdd);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<EntityDto> entityDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllEntitiesRequest()
      .executeAndReturnList(EntityDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(entityDtos).hasSize(totalNumberOfEntities);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }


  @Test
  public void testQueryPerformance_getAllCollectionEntities() {
    // given
    final int totalNumberOfEntities = getNumberOfEntities();
    // We consider 4 entity types for a collection: process reports / decision reports / combined reports / dashboards
    final int numOfEntityTypesForCollection = 4;
    final int numOfEachEntityToAdd = totalNumberOfEntities / numOfEntityTypesForCollection;
    // if the overall number is not divisible by the number of entity type, we add some extra dashboards
    final int extraDashboardsToAdd = totalNumberOfEntities % numOfEntityTypesForCollection;

    final String collectionId = addCollectionToOptimize();
    addSingleProcessReportsToOptimize(numOfEachEntityToAdd, collectionId);
    addSingleDecisionReportsToOptimize(numOfEachEntityToAdd, collectionId);
    addCombinedReportsToOptimize(numOfEachEntityToAdd, collectionId);
    addDashboardsToOptimize(numOfEachEntityToAdd + extraDashboardsToAdd, collectionId);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<EntityDto> entityDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetCollectionEntitiesRequest(collectionId)
      .executeAndReturnList(EntityDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(entityDtos).hasSize(totalNumberOfEntities);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  @Test
  public void testQueryPerformance_getAlerts() {
    // given
    final int numberOfAlerts = getNumberOfEntities();

    final String collectionId = addCollectionToOptimize();
    final List<String> reportIds = addSingleProcessReportsToOptimize(10, collectionId);
    addAlertsToOptimize(numberOfAlerts, reportIds);

    // when
    long startTimeMs = System.currentTimeMillis();
    final List<AlertDefinitionDto> entityDtos = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAlertsForCollectionRequest(collectionId)
      .executeAndReturnList(AlertDefinitionDto.class, Response.Status.OK.getStatusCode());
    long responseTimeMs = System.currentTimeMillis() - startTimeMs;

    // then
    assertThat(entityDtos).hasSize(numberOfAlerts);
    log.info("responseTime {}", responseTimeMs);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  private void addSingleProcessReportsToOptimize(final int numberOfReports) {
    addSingleProcessReportsToOptimize(numberOfReports, null);
  }

  private List<String> addSingleProcessReportsToOptimize(final int numberOfReports, final String collectionId) {
    final ProcessReportDataDto processReportData = TemplatedProcessReportDataBuilder.createReportData()
      .setReportDataType(ProcessReportDataType.RAW_DATA)
      .setProcessDefinitionKey(processDefinitionOptimizeDto.getKey())
      .setProcessDefinitionVersion(processDefinitionOptimizeDto.getVersion())
      .build();

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numberOfReports)
      .forEach(index -> {
        final SingleProcessReportDefinitionDto definition = new SingleProcessReportDefinitionDto(processReportData);
        definition.setCollectionId(collectionId);
        definition.setOwner(DEFAULT_USER);
        definition.setLastModifier(DEFAULT_USER);
        definition.setLastModified(OffsetDateTime.now());
        definition.setId(IdGenerator.getNextId());
        definitionsById.put(definition.getId(), definition);
      });
    addToElasticsearch(new SingleProcessReportIndex().getIndexName(), definitionsById);
    return new ArrayList<>(definitionsById.keySet());
  }

  private void addSingleDecisionReportsToOptimize(final int numberOfReports) {
    addSingleDecisionReportsToOptimize(numberOfReports, null);
  }

  private void addSingleDecisionReportsToOptimize(final int numberOfReports, final String collectionId) {
    final DecisionReportDataDto decisionReportData = DecisionReportDataBuilder.create()
      .setReportDataType(DecisionReportDataType.RAW_DATA)
      .setDecisionDefinitionKey(decisionDefinitionOptimizeDto.getKey())
      .setDecisionDefinitionVersion(decisionDefinitionOptimizeDto.getVersion())
      .build();
    final SingleDecisionReportDefinitionDto definition = new SingleDecisionReportDefinitionDto(decisionReportData);
    definition.setCollectionId(collectionId);
    definition.setOwner(DEFAULT_USER);
    definition.setLastModifier(DEFAULT_USER);
    definition.setLastModified(OffsetDateTime.now());

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numberOfReports)
      .forEach(index -> definitionsById.put(IdGenerator.getNextId(), definition));
    addToElasticsearch(new SingleDecisionReportIndex().getIndexName(), definitionsById);
  }

  private void addCombinedReportsToOptimize(final int numberOfReports) {
    addCombinedReportsToOptimize(numberOfReports, null);
  }

  private void addCombinedReportsToOptimize(final int numberOfReports, final String collectionId) {
    final CombinedReportDataDto reportData = new CombinedReportDataDto();
    reportData.setReports(Arrays.asList(
      new CombinedReportItemDto("firstReportId", "red"),
      new CombinedReportItemDto("secondReportId", "blue")
    ));
    final CombinedReportDefinitionDto definition = new CombinedReportDefinitionDto(new CombinedReportDataDto());
    definition.setCollectionId(collectionId);
    definition.setOwner(DEFAULT_USER);
    definition.setLastModifier(DEFAULT_USER);
    definition.setLastModified(OffsetDateTime.now());

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numberOfReports)
      .forEach(index -> definitionsById.put(IdGenerator.getNextId(), definition));
    addToElasticsearch(new CombinedReportIndex().getIndexName(), definitionsById);
  }

  private void addDashboardsToOptimize(final int numOfDashboards) {
    addDashboardsToOptimize(numOfDashboards, null);
  }

  private void addDashboardsToOptimize(final int numOfDashboards, final String collectionId) {
    DashboardDefinitionDto definition = new DashboardDefinitionDto();
    definition.setReports(Arrays.asList(
      ReportLocationDto.builder().id("firstReportId").build(),
      ReportLocationDto.builder().id("secondReportId").build()
    ));
    definition.setCollectionId(collectionId);
    definition.setOwner(DEFAULT_USER);
    definition.setLastModifier(DEFAULT_USER);
    definition.setLastModified(OffsetDateTime.now());

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numOfDashboards)
      .forEach(index -> definitionsById.put(IdGenerator.getNextId(), definition));
    addToElasticsearch(new DashboardIndex().getIndexName(), definitionsById);
  }

  private String addCollectionToOptimize() {
    CollectionDataDto collectionData = createCollectionData();
    CollectionDefinitionDto definition = createCollectionDefinition(collectionData);
    addToElasticsearch(new CollectionIndex().getIndexName(), ImmutableMap.of(definition.getId(), definition));
    return definition.getId();
  }

  private void addCollectionsToOptimize(final int numOfCollections) {
    CollectionDataDto collectionData = createCollectionData();

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numOfCollections)
      .forEach(index -> {
        CollectionDefinitionDto definition = createCollectionDefinition(collectionData);
        definitionsById.put(definition.getId(), definition);
      });
    addToElasticsearch(new CollectionIndex().getIndexName(), definitionsById);

    // We add up to one of each entity randomly to each collection we have created
    for (String collectionId : definitionsById.keySet()) {
      if (RandomUtils.nextBoolean()) {
        addSingleProcessReportsToOptimize(1, collectionId);
      }
      if (RandomUtils.nextBoolean()) {
        addSingleDecisionReportsToOptimize(1, collectionId);
      }
      if (RandomUtils.nextBoolean()) {
        addCombinedReportsToOptimize(1, collectionId);
      }
      if (RandomUtils.nextBoolean()) {
        addDashboardsToOptimize(1, collectionId);
      }
    }
  }

  private void addAlertsToOptimize(final int numberOfAlerts, final List<String> reportIds) {
    final AlertDefinitionDto definition = new AlertDefinitionDto();
    definition.setReportId(reportIds.get(RandomUtils.nextInt(0, reportIds.size())));
    definition.setOwner(DEFAULT_USER);
    definition.setLastModifier(DEFAULT_USER);
    definition.setLastModified(OffsetDateTime.now());

    Map<String, Object> definitionsById = new HashMap<>();
    IntStream.range(0, numberOfAlerts)
      .forEach(index -> definitionsById.put(IdGenerator.getNextId(), definition));
    addToElasticsearch(new AlertIndex().getIndexName(), definitionsById);
  }

  private ProcessDefinitionOptimizeDto addProcessDefinitionToOptimize() {
    final ProcessDefinitionOptimizeDto processDefinition = createProcessDefinition();
    addToElasticsearch(
      new ProcessDefinitionIndex().getIndexName(),
      ImmutableMap.of(processDefinition.getId(), processDefinition)
    );
    return processDefinition;
  }

  private DecisionDefinitionOptimizeDto addDecisionDefinitionToOptimize() {
    final DecisionDefinitionOptimizeDto decisionDefinition = createDecisionDefinition();
    addToElasticsearch(
      new DecisionDefinitionIndex().getIndexName(),
      ImmutableMap.of(decisionDefinition.getId(), decisionDefinition)
    );
    return decisionDefinition;
  }

  private CollectionDataDto createCollectionData() {
    CollectionDataDto collectionData = new CollectionDataDto();
    collectionData.setScope(Arrays.asList(
      new CollectionScopeEntryDto(DefinitionType.PROCESS, processDefinitionOptimizeDto.getKey()),
      new CollectionScopeEntryDto(DefinitionType.DECISION, decisionDefinitionOptimizeDto.getKey())
    ));
    final CollectionRoleDto collectionRoleDto = new CollectionRoleDto(
      new IdentityDto(DEFAULT_USER, IdentityType.USER),
      RoleType.MANAGER
    );
    collectionData.setRoles(Collections.singletonList(collectionRoleDto));
    return collectionData;
  }

  private CollectionDefinitionDto createCollectionDefinition(final CollectionDataDto collectionData) {
    CollectionDefinitionDto definition = new CollectionDefinitionDto();
    definition.setLastModifier(DEFAULT_USER);
    definition.setLastModified(OffsetDateTime.now());
    definition.setData(collectionData);
    definition.setId(IdGenerator.getNextId());
    return definition;
  }

  private static ProcessDefinitionOptimizeDto createProcessDefinition() {
    return createProcessDefinition("key", "1", null, "processName", DEFAULT_ENGINE_ALIAS);
  }

  private static DecisionDefinitionOptimizeDto createDecisionDefinition() {
    return createDecisionDefinition("key", "1", null, "decisionName", DEFAULT_ENGINE_ALIAS);
  }

  private void addTenantsToElasticsearch() {
    final TenantDto tenantDto = new TenantDto("null", "null", DEFAULT_ENGINE_ALIAS);
    elasticSearchIntegrationTestExtension.addEntriesToElasticsearch(
      TENANT_INDEX_NAME,
      ImmutableMap.of(tenantDto.getId(), tenantDto)
    );
  }

}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.engine.mediator;

import lombok.Setter;
import org.camunda.optimize.dto.engine.HistoricActivityInstanceEngineDto;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.CamundaEventImportService;
import org.camunda.optimize.service.es.writer.CompletedActivityInstanceWriter;
import org.camunda.optimize.service.importing.TimestampBasedImportMediator;
import org.camunda.optimize.service.importing.engine.fetcher.instance.CompletedActivityInstanceFetcher;
import org.camunda.optimize.service.importing.engine.handler.CompletedActivityInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerRegistry;
import org.camunda.optimize.service.importing.engine.service.CompletedActivityInstanceImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CompletedActivityInstanceEngineImportMediator
  extends TimestampBasedImportMediator<CompletedActivityInstanceImportIndexHandler, HistoricActivityInstanceEngineDto> {

  private CompletedActivityInstanceFetcher engineEntityFetcher;

  @Autowired
  private CompletedActivityInstanceWriter completedActivityInstanceWriter;
  @Autowired
  private CamundaEventImportService camundaEventService;
  @Autowired
  @Setter
  private EngineImportIndexHandlerRegistry importIndexHandlerRegistry;

  private final EngineContext engineContext;

  public CompletedActivityInstanceEngineImportMediator(final EngineContext engineContext) {
    this.engineContext = engineContext;
  }

  @PostConstruct
  public void init() {
    importIndexHandler = importIndexHandlerRegistry.getCompletedActivityInstanceImportIndexHandler(engineContext.getEngineAlias());
    engineEntityFetcher = beanFactory.getBean(CompletedActivityInstanceFetcher.class, engineContext);
    importService = new CompletedActivityInstanceImportService(
      completedActivityInstanceWriter,
      camundaEventService,
      elasticsearchImportJobExecutor,
      engineContext
    );
  }

  @Override
  protected List<HistoricActivityInstanceEngineDto> getEntitiesLastTimestamp() {
    return engineEntityFetcher.fetchCompletedActivityInstancesForTimestamp(importIndexHandler.getTimestampOfLastEntity());
  }

  @Override
  protected List<HistoricActivityInstanceEngineDto> getEntitiesNextPage() {
    return engineEntityFetcher.fetchCompletedActivityInstances(importIndexHandler.getNextPage());
  }

  @Override
  protected int getMaxPageSize() {
    return configurationService.getEngineImportActivityInstanceMaxPageSize();
  }

  @Override
  protected OffsetDateTime getTimestamp(final HistoricActivityInstanceEngineDto dto) {
    return dto.getEndTime();
  }

}

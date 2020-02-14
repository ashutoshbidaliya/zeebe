/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.engine.mediator;

import org.camunda.optimize.dto.engine.HistoricVariableUpdateInstanceDto;
import org.camunda.optimize.plugin.ImportAdapterProvider;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.service.CamundaEventImportService;
import org.camunda.optimize.service.es.writer.variable.ProcessVariableUpdateWriter;
import org.camunda.optimize.service.importing.TimestampBasedImportMediator;
import org.camunda.optimize.service.importing.engine.fetcher.instance.VariableUpdateInstanceFetcher;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerRegistry;
import org.camunda.optimize.service.importing.engine.handler.VariableUpdateInstanceImportIndexHandler;
import org.camunda.optimize.service.importing.engine.service.VariableUpdateInstanceImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VariableUpdateEngineImportMediator
  extends TimestampBasedImportMediator<VariableUpdateInstanceImportIndexHandler, HistoricVariableUpdateInstanceDto> {

  private VariableUpdateInstanceFetcher engineEntityFetcher;

  @Autowired
  private CamundaEventImportService camundaEventService;
  @Autowired
  private ProcessVariableUpdateWriter variableWriter;
  @Autowired
  private ImportAdapterProvider importAdapterProvider;
  @Autowired
  private EngineImportIndexHandlerRegistry importIndexHandlerRegistry;

  private final EngineContext engineContext;

  public VariableUpdateEngineImportMediator(final EngineContext engineContext) {
    this.engineContext = engineContext;
  }

  @PostConstruct
  public void init() {
    importIndexHandler = importIndexHandlerRegistry.getRunningVariableInstanceImportIndexHandler(engineContext.getEngineAlias());
    engineEntityFetcher = beanFactory.getBean(VariableUpdateInstanceFetcher.class, engineContext);
    importService = new VariableUpdateInstanceImportService(
      variableWriter, camundaEventService, importAdapterProvider, elasticsearchImportJobExecutor, engineContext
    );
  }

  @Override
  protected List<HistoricVariableUpdateInstanceDto> getEntitiesNextPage() {
    return engineEntityFetcher.fetchVariableInstanceUpdates(importIndexHandler.getNextPage());
  }

  @Override
  protected List<HistoricVariableUpdateInstanceDto> getEntitiesLastTimestamp() {
    return engineEntityFetcher.fetchVariableInstanceUpdates(importIndexHandler.getTimestampOfLastEntity());
  }

  @Override
  protected int getMaxPageSize() {
    return configurationService.getEngineImportVariableInstanceMaxPageSize();
  }

  @Override
  protected OffsetDateTime getTimestamp(final HistoricVariableUpdateInstanceDto historicVariableUpdateInstanceDto) {
    return historicVariableUpdateInstanceDto.getTime();
  }
}

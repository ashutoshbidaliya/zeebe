/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.engine;

import lombok.RequiredArgsConstructor;
import org.camunda.optimize.rest.engine.EngineContext;
import org.camunda.optimize.rest.engine.EngineContextFactory;
import org.camunda.optimize.service.importing.EngineImportMediator;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerProvider;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerRegistry;
import org.camunda.optimize.service.importing.engine.handler.EngineImportIndexHandlerRegistry;
import org.camunda.optimize.service.importing.engine.mediator.CompletedActivityInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.CompletedProcessInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.CompletedUserTaskEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.DecisionDefinitionEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.DecisionDefinitionXmlEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.DecisionInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.IdentityLinkLogEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.ProcessDefinitionEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.ProcessDefinitionXmlEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.RunningActivityInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.RunningProcessInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.RunningUserTaskInstanceEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.StoreIndexesEngineImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.TenantImportMediator;
import org.camunda.optimize.service.importing.engine.mediator.VariableUpdateEngineImportMediator;
import org.camunda.optimize.service.util.configuration.ConfigurationReloadable;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class EngineImportSchedulerFactory implements ConfigurationReloadable {
  private static final Logger logger = LoggerFactory.getLogger(EngineImportSchedulerFactory.class);

  private final EngineImportIndexHandlerRegistry importIndexHandlerRegistry;
  private final BeanFactory beanFactory;
  private final EngineContextFactory engineContextFactory;
  private final ConfigurationService configurationService;

  private List<EngineImportScheduler> schedulers;

  private List<EngineImportScheduler> buildSchedulers() {
    final List<EngineImportScheduler> result = new ArrayList<>();

    for (EngineContext engineContext : engineContextFactory.getConfiguredEngines()) {
      try {
        final List<EngineImportMediator> mediators = createMediatorList(engineContext);
        final EngineImportScheduler scheduler = new EngineImportScheduler(
          mediators,
          engineContext.getEngineAlias()
        );

        if (!configurationService.isEngineImportEnabled(engineContext.getEngineAlias())) {
          logger.info("Engine import was disabled by config for engine with alias {}.", engineContext.getEngineAlias());
          scheduler.disable();
        }

        result.add(scheduler);
      } catch (Exception e) {
        logger.error("Can't create scheduler for engine [{}]", engineContext.getEngineAlias(), e);
      }
    }

    return result;
  }

  private List<EngineImportMediator> createMediatorList(EngineContext engineContext) {
    List<EngineImportMediator> mediators = new ArrayList<>();
    importIndexHandlerRegistry.register(
      engineContext.getEngineAlias(),
      beanFactory.getBean(EngineImportIndexHandlerProvider.class, engineContext)
    );

    // tenant import first
    mediators.add(
      beanFactory.getBean(TenantImportMediator.class, engineContext)
    );

    // definition imports come first in line,
    mediators.add(
      beanFactory.getBean(ProcessDefinitionEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(ProcessDefinitionXmlEngineImportMediator.class, engineContext)
    );
    if (configurationService.getImportDmnDataEnabled()) {
      mediators.add(
        beanFactory.getBean(DecisionDefinitionEngineImportMediator.class, engineContext)
      );
      mediators.add(
        beanFactory.getBean(DecisionDefinitionXmlEngineImportMediator.class, engineContext)
      );
    }

    // so potential dependencies by the instance and activity import on existing definition data are likely satisfied
    mediators.add(
      beanFactory.getBean(CompletedActivityInstanceEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(RunningActivityInstanceEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(CompletedProcessInstanceEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(StoreIndexesEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(RunningProcessInstanceEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(VariableUpdateEngineImportMediator.class, engineContext)
    );
    mediators.add(
      beanFactory.getBean(CompletedUserTaskEngineImportMediator.class, engineContext)
    );
    if (configurationService.getImportUserTaskWorkerDataEnabled()) {
      mediators.add(
        beanFactory.getBean(IdentityLinkLogEngineImportMediator.class, engineContext)
      );
    }
    mediators.add(
      beanFactory.getBean(RunningUserTaskInstanceEngineImportMediator.class, engineContext)
    );

    if (configurationService.getImportDmnDataEnabled()) {
      mediators.add(
        beanFactory.getBean(DecisionInstanceEngineImportMediator.class, engineContext)
      );
    }

    return mediators;
  }

  public List<EngineImportScheduler> getImportSchedulers() {
    if (schedulers == null) {
      this.schedulers = this.buildSchedulers();
    }
    return schedulers;
  }

  @PreDestroy
  public void shutdown() {
    if (schedulers != null) {
      for (EngineImportScheduler oldScheduler : schedulers) {
        oldScheduler.disable();
        oldScheduler.shutdown();
      }
    }
    engineContextFactory.close();
  }

  @Override
  public void reloadConfiguration(ApplicationContext context) {
    shutdown();
    engineContextFactory.init();
    importIndexHandlerRegistry.reloadConfiguration();
    schedulers = this.buildSchedulers();
  }
}

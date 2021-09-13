/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing.event.handler;

import lombok.AllArgsConstructor;
import org.camunda.optimize.service.importing.TimestampBasedEngineImportIndexHandler;
import org.camunda.optimize.upgrade.es.ElasticsearchConstants;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@AllArgsConstructor
public class CamundaEventTraceImportIndexHandler extends TimestampBasedEngineImportIndexHandler {

  private final String definitionKey;

  @Override
  public String getEngineAlias() {
    return ElasticsearchConstants.ENGINE_ALIAS_OPTIMIZE;
  }

  @Override
  protected String getElasticsearchDocID() {
    return ElasticsearchConstants.EVENT_PROCESSING_IMPORT_REFERENCE_PREFIX + definitionKey;
  }
}

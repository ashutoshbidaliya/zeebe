/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import type {
  ProcessDefinition,
  QueryProcessDefinitionsResponseBody,
} from '@vzeta/camunda-api-zod-schemas/operate';
import {DEFAULT_TENANT_ID} from 'common/multitenancy/constants';
import {uniqueId} from './utils';

function getProcessDefinitionMock(
  customFields: Partial<ProcessDefinition> = {},
): ProcessDefinition {
  const id = uniqueId.next().value;

  return {
    processDefinitionId: `process${id}`,
    processDefinitionKey: id,
    tenantId: DEFAULT_TENANT_ID,
    version: 1,
    name: `Process ${id}`,
    ...customFields,
  };
}

function getQueryProcessDefinitionsResponseMock(
  processDefinitions: ProcessDefinition[],
  totalItems: number = processDefinitions.length,
): QueryProcessDefinitionsResponseBody {
  return {
    items: processDefinitions,
    page: {
      totalItems,
      firstSortValues: [0, 1],
      lastSortValues: [2, 3],
    },
  };
}

export {getQueryProcessDefinitionsResponseMock, getProcessDefinitionMock};

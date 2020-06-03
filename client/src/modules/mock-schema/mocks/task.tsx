/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {Task} from 'modules/types';

const unclaimedTask: Task = {
  key: '1',
  name: 'Unclaimed Task',
  workflowName: 'Nice Workflow',
  assignee: null,
  creationTime: new Date('2019').toISOString(),
  completionTime: null,
  variables: [],
  taskState: 'CREATED',
};

const completedTask: Task = {
  key: '0',
  name: 'My Completed Task',
  workflowName: 'Cool Workflow',
  assignee: {
    firstname: 'Jules',
    lastname: 'Verne',
    username: 'julesverne',
  },
  creationTime: new Date('2019').toISOString(),
  completionTime: new Date('2020').toISOString(),
  variables: [],
  taskState: 'COMPLETED',
};

export {unclaimedTask, completedTask};

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {Task} from 'modules/types';
import {TaskStates} from 'modules/constants/taskStates';
import {currentUser} from '../constants/currentUser';

const unclaimedTask: Task = {
  __typename: 'Task',
  id: '0',
  name: 'My Task',
  workflowName: 'Nice Workflow',
  assignee: null,
  creationTime: new Date('2019').toISOString(),
  completionTime: null,
  variables: [],
  taskState: TaskStates.Created,
};

const claimedTask: Task = {
  ...unclaimedTask,
  assignee: currentUser,
};

const completedTask: Task = {
  ...claimedTask,

  completionTime: new Date('2020').toISOString(),
  taskState: TaskStates.Completed,
};

export {unclaimedTask, completedTask, claimedTask};

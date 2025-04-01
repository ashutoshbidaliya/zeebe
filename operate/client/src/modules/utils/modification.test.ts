/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {finishMovingToken} from './modifications';
import {modificationsStore} from 'modules/stores/modifications';
import {isMultiInstance} from 'modules/bpmn-js/utils/isMultiInstance';
import {tracking} from 'modules/tracking';

jest.mock('modules/stores/modifications', () => ({
  modificationsStore: {
    state: {
      sourceFlowNodeIdForMoveOperation: null,
      sourceFlowNodeInstanceKeyForMoveOperation: null,
      status: 'disabled',
    },
    addMoveModification: jest.fn(),
  },
}));

jest.mock('modules/bpmn-js/utils/isMultiInstance', () => ({
  isMultiInstance: jest.fn(),
}));

jest.mock('modules/tracking', () => ({
  tracking: {
    track: jest.fn(),
  },
}));

describe('finishMovingToken', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    modificationsStore.state.sourceFlowNodeIdForMoveOperation = 'sourceNode';
    modificationsStore.state.sourceFlowNodeInstanceKeyForMoveOperation = null;
  });

  it('should track the move-token event', () => {
    finishMovingToken(5, 3, 'targetNode');

    expect(tracking.track).toHaveBeenCalledWith({
      eventName: 'move-token',
    });
  });

  it('should add a move modification when targetFlowNodeId is provided', () => {
    (isMultiInstance as jest.Mock).mockReturnValue(false);

    finishMovingToken(5, 3, 'targetNode');

    expect(modificationsStore.addMoveModification).toHaveBeenCalledWith({
      sourceFlowNodeId: 'sourceNode',
      sourceFlowNodeInstanceKey: undefined,
      targetFlowNodeId: 'targetNode',
      affectedTokenCount: 5,
      visibleAffectedTokenCount: 3,
      newScopeCount: 5,
    });
  });

  it('should set newScopeCount to 1 for multi-instance source nodes', () => {
    (isMultiInstance as jest.Mock).mockReturnValue(true);

    finishMovingToken(5, 3, 'targetNode');

    expect(modificationsStore.addMoveModification).toHaveBeenCalledWith(
      expect.objectContaining({
        newScopeCount: 1,
      }),
    );
  });

  it('should not add a move modification if targetFlowNodeId is undefined', () => {
    finishMovingToken(5, 3);

    expect(modificationsStore.addMoveModification).not.toHaveBeenCalled();
  });

  it('should reset the modification state after finishing the move', () => {
    finishMovingToken(5, 3, 'targetNode');

    expect(modificationsStore.state.status).toBe('enabled');
    expect(
      modificationsStore.state.sourceFlowNodeIdForMoveOperation,
    ).toBeNull();
    expect(
      modificationsStore.state.sourceFlowNodeInstanceKeyForMoveOperation,
    ).toBeNull();
  });
});

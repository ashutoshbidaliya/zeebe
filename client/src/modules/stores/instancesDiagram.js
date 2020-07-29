/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {observable, decorate, action, computed} from 'mobx';
import {fetchWorkflowXML} from 'modules/api/diagram';
import {parseDiagramXML} from 'modules/utils/bpmn';
import {getFlowNodes} from 'modules/utils/flowNodes';

const DEFAULT_STATE = {
  diagramModel: null,
  isInitialLoadComplete: false,
  isLoading: false,
  isFailed: false,
};

class InstancesDiagram {
  state = {...DEFAULT_STATE};

  fetchWorkflowXml = async (workflowId) => {
    this.startLoading();
    const response = await fetchWorkflowXML(workflowId);
    if (response.error) {
      this.handleFailure();
    } else {
      this.handleSuccess(await parseDiagramXML(response));
    }

    if (!this.state.isInitialLoadComplete) {
      this.completeInitialLoad();
    }
  };

  get selectableFlowNodes() {
    return getFlowNodes(this.state.diagramModel?.bpmnElements);
  }

  get selectableIds() {
    return this.selectableFlowNodes.map(({id}) => id);
  }

  resetDiagramModel = () => {
    this.state.diagramModel = null;
  };

  completeInitialLoad = () => {
    this.state.isInitialLoadComplete = true;
  };

  startLoading = () => {
    this.state.isLoading = true;
  };

  handleSuccess = (parsedDiagramXml) => {
    this.state.diagramModel = parsedDiagramXml;
    this.state.isLoading = false;
    this.state.isFailed = false;
  };

  handleFailure = () => {
    this.state.isLoading = false;
    this.state.isFailed = true;
  };

  reset = () => {
    this.state = {...DEFAULT_STATE};
  };
}

decorate(InstancesDiagram, {
  state: observable,
  reset: action,
  resetDiagramModel: action,
  startLoading: action,
  handleSuccess: action,
  handleFailure: action,
  completeInitialLoad: action,
  selectableFlowNodes: computed,
  selectableIds: computed,
});

export const instancesDiagram = new InstancesDiagram();

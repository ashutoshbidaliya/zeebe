import {jsx} from 'view-utils';
import {mountTemplate, triggerEvent, createMockComponent} from 'testHelpers';
import {expect} from 'chai';
import sinon from 'sinon';
import {Statistics, __set__, __ResetDependency__} from 'main/processDisplay/statistics/Statistics';

describe('<Statistics>', () => {
  let node;
  let update;
  let leaveGatewayAnalysisMode;
  let unopenedState;
  let openedState;
  let StatisticChart;
  let getBpmnViewer;
  let viewer;
  let findSequenceFlowBetweenGatewayAndActivity;
  const sequenceFlow = 'SEQUENCE_FLOW';

  beforeEach(() => {
    unopenedState = {
      diagram: {
        selection: {}
      },
      statistics: {
        correlation: {}
      }
    };

    openedState = {
      diagram: {
        selection: {
          endEvent: 'a',
          gateway: 'b'
        }
      },
      statistics: {
        correlation: {
          data: {
            followingNodes: {
              foo: 12,
              bar: 1
            }
          }
        }
      }
    };

    viewer = {
      get: sinon.stub().returnsThis(),
      forEach: sinon.stub().callsArgWith(0, sequenceFlow),
      removeMarker: sinon.spy(),
      addMarker: sinon.spy()
    };
    getBpmnViewer = sinon.stub().returns(viewer);

    findSequenceFlowBetweenGatewayAndActivity = sinon.stub().returns(sequenceFlow);
    __set__('findSequenceFlowBetweenGatewayAndActivity', findSequenceFlowBetweenGatewayAndActivity);

    StatisticChart = createMockComponent('StatisticChart');
    __set__('StatisticChart', StatisticChart);

    leaveGatewayAnalysisMode = sinon.spy();
    __set__('leaveGatewayAnalysisMode', leaveGatewayAnalysisMode);

    ({node, update} = mountTemplate(<Statistics getBpmnViewer={getBpmnViewer} />));
  });

  afterEach(() => {
    __ResetDependency__('leaveGatewayAnalysisMode');
    __ResetDependency__('StatisticChart');
    __ResetDependency__('findSequenceFlowBetweenGatewayAndActivity');
  });

  it('should not have the open class if gateway is not set', () => {
    update(unopenedState);

    const container = node.querySelector('.statisticsContainer');

    expect(Array.from(container.classList)).to.not.include('open');
  });

  it('should have the open class when endEvent and gateway are set', () => {
    update(openedState);

    const container = node.querySelector('.statisticsContainer');

    expect(Array.from(container.classList)).to.include('open');
  });

  it('should leave gateway analysis mode when close button is clicked', () => {
    update(openedState);

    triggerEvent({
      node: node,
      selector: '.close',
      eventName: 'click'
    });

    expect(leaveGatewayAnalysisMode.called).to.eql(true);
  });

  it('should contain StatisticCharts', () => {
    update(openedState);

    expect(node.textContent).to.contain('StatisticChart');
  });

  it('should highlight sequence flows on hover', () => {
    update(openedState);

    StatisticChart.getAttribute('chartConfig').onHover(true)({}, 0);

    expect(viewer.addMarker.calledWith(sequenceFlow, 'chart-hover')).to.eql(true);
  });

  it('should clear all highlights on any hover action', () => {
    update(openedState);

    StatisticChart.getAttribute('chartConfig').onHover(false)({}, 0);

    expect(viewer.removeMarker.calledWith(sequenceFlow, 'chart-hover')).to.eql(true);
  });
});

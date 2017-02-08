import {expect} from 'chai';
import {reducer, createOpenDateFilterModalAction,
        createCloseDateFilterModalAction} from 'main/processDisplay/controls/filterCreation/reducer';
import {INITIAL_STATE, LOADING_STATE, LOADED_STATE} from 'utils/loading';

describe('ProcessDefinition reducer', () => {
  let open;

  it('should set open to true on open action', () => {
    ({open} = reducer(undefined, createOpenDateFilterModalAction()));

    expect(open).to.eql(true);
  });

  it('should set open to false on open action', () => {
    ({open} = reducer(undefined, createCloseDateFilterModalAction()));

    expect(open).to.eql(false);
  });
});

import React from 'react';
import PropTypes from 'prop-types';

import SplitPane from 'modules/components/SplitPane';
import {EXPAND_STATE, SORT_ORDER, DEFAULT_SORTING} from 'modules/constants';
import {isEmpty, isEqual} from 'modules/utils';
import {parseFilterForRequest} from 'modules/utils/filter';
import {fetchWorkflowInstances} from 'modules/api/instances';

import List from './List';
import ListFooter from './ListFooter';
import * as Styled from './styled';

export default class ListView extends React.Component {
  static propTypes = {
    selection: PropTypes.object.isRequired,
    filterCount: PropTypes.number.isRequired,
    onUpdateSelection: PropTypes.func.isRequired,
    filter: PropTypes.object.isRequired,
    openSelection: PropTypes.number,
    expandState: PropTypes.oneOf(Object.values(EXPAND_STATE)),
    errorMessage: PropTypes.string,
    selections: PropTypes.array,
    onAddNewSelection: PropTypes.func,
    onAddToSpecificSelection: PropTypes.func,
    onAddToOpenSelection: PropTypes.func
  };

  state = {
    firstElement: 0,
    instances: [],
    entriesPerPage: 0,
    sorting: DEFAULT_SORTING
  };

  componentDidMount() {
    !isEmpty(this.props.filter) && this.loadData();
  }

  componentDidUpdate(prevProps, prevState) {
    // set firstElement to 0 when filter changes
    if (
      prevProps.filter !== this.props.filter &&
      this.state.firstElement !== 0
    ) {
      return this.setState({firstElement: 0});
    }

    const hasFilterChanged = prevProps.filter !== this.props.filter;
    const hasFirstElementChanged =
      prevState.firstElement !== this.state.firstElement;
    const hasSortingChanged = !isEqual(prevState.sorting, this.state.sorting);

    if (hasFilterChanged || hasFirstElementChanged || hasSortingChanged) {
      this.loadData();
    }
  }

  loadData = async () => {
    const instances = await fetchWorkflowInstances({
      queries: [
        {
          ...parseFilterForRequest(this.props.filter)
        }
      ],
      sorting: this.state.sorting,
      firstResult: this.state.firstElement,
      maxResults: 50
    });
    this.setState({
      instances: instances.workflowInstances
    });
  };

  // (1) should make state sort order asc if key is currently sorted by in desc order
  handleSorting = key => {
    const {
      sorting: {sortBy: currentSortBy, sortOrder: currentSortOrder}
    } = this.state;

    let newSorting = {sortBy: key, sortOrder: SORT_ORDER.DESC};

    if (currentSortBy === key && currentSortOrder === SORT_ORDER.DESC) {
      newSorting.sortOrder = SORT_ORDER.ASC;
    }

    return this.setState({sorting: newSorting});
  };

  render() {
    const {
      selection,
      filter,
      expandState,
      onUpdateSelection,
      filterCount,
      onAddToOpenSelection,
      onAddNewSelection,
      onAddToSpecificSelection
    } = this.props;
    return (
      <SplitPane.Pane {...this.props}>
        <SplitPane.Pane.Header>Instances</SplitPane.Pane.Header>
        <Styled.PaneBody>
          {!isEmpty(this.props.filter) && (
            <List
              data={this.state.instances}
              selection={selection}
              filterCount={filterCount}
              filter={filter}
              expandState={expandState}
              sorting={this.state.sorting}
              handleSorting={this.handleSorting}
              onUpdateSelection={onUpdateSelection}
              onEntriesPerPageChange={entriesPerPage =>
                this.setState({entriesPerPage})
              }
            />
          )}
        </Styled.PaneBody>
        <SplitPane.Pane.Footer>
          {!isEmpty(this.props.filter) && (
            <ListFooter
              filterCount={filterCount}
              perPage={this.state.entriesPerPage}
              firstElement={this.state.firstElement}
              selection={this.props.selection}
              selections={this.props.selections}
              openSelection={this.props.openSelection}
              onAddToSpecificSelection={onAddToSpecificSelection}
              onAddToOpenSelection={onAddToOpenSelection}
              onAddNewSelection={onAddNewSelection}
              onFirstElementChange={firstElement =>
                this.setState({firstElement})
              }
            />
          )}
        </SplitPane.Pane.Footer>
      </SplitPane.Pane>
    );
  }
}

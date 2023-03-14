/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {useEffect, useRef} from 'react';
import {
  EmptyMessage,
  ListContainer,
  Container,
  EmptyMessageFirstLine,
  EmptyMessageSecondLine,
  EmptyMessageText,
  EmptyListIcon,
} from './styled';
import {Task} from './Task';
import {Stack} from '@carbon/react';
import {Skeleton} from './Skeleton';
import {QueryTask} from 'modules/queries/get-tasks';
import {useTaskFilters} from 'modules/hooks/useTaskFilters';

type Props = {
  onScrollUp: () => Promise<ReadonlyArray<QueryTask>>;
  onScrollDown: () => Promise<ReadonlyArray<QueryTask>>;
  tasks: ReadonlyArray<QueryTask>;
  loading: boolean;
};

const AvailableTasks: React.FC<Props> = ({
  loading,
  onScrollDown,
  onScrollUp,
  tasks,
}) => {
  const taskRef = useRef<HTMLDivElement>(null);
  const scrollableListRef = useRef<HTMLDivElement>(null);
  const {filter} = useTaskFilters();

  useEffect(() => {
    scrollableListRef?.current?.scrollTo?.(0, 0);
  }, [filter]);

  return (
    <Container
      $enablePadding={tasks.length === 0 && !loading}
      title="Available tasks"
    >
      {loading && <Skeleton />}
      {tasks.length > 0 && (
        <ListContainer
          data-testid="scrollable-list"
          ref={scrollableListRef}
          onScroll={async (event) => {
            const target = event.target as HTMLDivElement;

            if (
              target.scrollHeight - target.clientHeight - target.scrollTop <=
              0
            ) {
              await onScrollDown();
            } else if (target.scrollTop === 0) {
              const previousTasks = await onScrollUp();

              target.scrollTop =
                (taskRef?.current?.clientHeight ?? 0) * previousTasks.length;
            }
          }}
          tabIndex={-1}
        >
          {tasks.map((task) => {
            return (
              <Task
                ref={taskRef}
                key={task.id}
                taskId={task.id}
                name={task.name}
                processName={task.processName}
                assignee={task.assignee}
                creationTime={task.creationTime}
                followUpDate={task.followUpDate}
                dueDate={task.dueDate}
              />
            );
          })}
        </ListContainer>
      )}
      {tasks.length === 0 && !loading && (
        <Stack as={EmptyMessage} gap={5} orientation="horizontal">
          <EmptyListIcon size={24} alt="" />
          <Stack gap={1} as={EmptyMessageText}>
            <EmptyMessageFirstLine>No tasks found</EmptyMessageFirstLine>
            <EmptyMessageSecondLine>
              There are no tasks matching your filter criteria.
            </EmptyMessageSecondLine>
          </Stack>
        </Stack>
      )}
    </Container>
  );
};

export {AvailableTasks};

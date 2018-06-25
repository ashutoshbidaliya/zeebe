package org.camunda.operate.zeebe;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.camunda.operate.entities.WorkflowInstanceEntity;
import org.camunda.operate.entities.WorkflowInstanceState;
import org.camunda.operate.es.writer.EntityStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.client.api.subscription.WorkflowInstanceEventHandler;


@Component
public class WorkflowInstanceEventTransformer extends AbstractEventTransformer implements WorkflowInstanceEventHandler {

  private Logger logger = LoggerFactory.getLogger(WorkflowInstanceEventTransformer.class);

  private static final Set<io.zeebe.client.api.events.WorkflowInstanceState> STATES = new HashSet<>();
  private static final Set<io.zeebe.client.api.events.WorkflowInstanceState> END_STATES = new HashSet<>();

  static {
    END_STATES.add(io.zeebe.client.api.events.WorkflowInstanceState.COMPLETED);
    END_STATES.add(io.zeebe.client.api.events.WorkflowInstanceState.CANCELED);

    STATES.add(io.zeebe.client.api.events.WorkflowInstanceState.CREATED);
    STATES.addAll(END_STATES);
  }

  @Autowired
  private EntityStorage entityStorage;

  @Override
  public void onWorkflowInstanceEvent(WorkflowInstanceEvent event) throws Exception {
    if (STATES.contains(event.getState())) {

      logger.debug(event.toJson());

      WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
      entity.setId(String.valueOf(event.getWorkflowInstanceKey()));
      entity.setWorkflowId(String.valueOf(event.getWorkflowKey()));
      entity.setBusinessKey(event.getBpmnProcessId());
      if (END_STATES.contains(event.getState())) {
        entity.setEndDate(OffsetDateTime.ofInstant(event.getMetadata().getTimestamp(), ZoneOffset.UTC));
        entity.setState(WorkflowInstanceState.COMPLETED);
      } else {
        entity.setState(WorkflowInstanceState.ACTIVE);
        entity.setStartDate(OffsetDateTime.ofInstant(event.getMetadata().getTimestamp(), ZoneOffset.UTC));
      }

      updateMetdataFields(entity, event);

      //TODO will wait till capacity available, can throw InterruptedException
      entityStorage.getOperateEntititesQueue(event.getMetadata().getTopicName()).put(entity);
    }
  }

}

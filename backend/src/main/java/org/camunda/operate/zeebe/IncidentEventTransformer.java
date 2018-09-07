package org.camunda.operate.zeebe;

import java.util.HashSet;
import java.util.Set;
import org.camunda.operate.entities.EventEntity;
import org.camunda.operate.entities.EventMetadataEntity;
import org.camunda.operate.entities.IncidentEntity;
import org.camunda.operate.es.writer.EntityStorage;
import org.camunda.operate.util.IdUtil;
import org.camunda.operate.util.ZeebeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.zeebe.client.api.events.IncidentEvent;
import io.zeebe.client.api.events.IncidentState;
import io.zeebe.client.api.record.Record;
import io.zeebe.client.api.record.RecordMetadata;
import io.zeebe.client.api.subscription.IncidentEventHandler;
import static org.camunda.operate.entities.IncidentState.fromZeebeIncidentState;


@Component
public class IncidentEventTransformer extends AbstractEventTransformer implements IncidentEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(IncidentEventTransformer.class);

  private final static Set<IncidentState> EVENTS = new HashSet<>();

  static {
    EVENTS.add(IncidentState.CREATED);
    EVENTS.add(IncidentState.RESOLVED);
    EVENTS.add(IncidentState.DELETED);
  }

  @Autowired
  private EntityStorage entityStorage;

  @Override
  public void onIncidentEvent(IncidentEvent event) throws Exception {

    ZeebeUtil.ALL_EVENTS_LOGGER.debug(event.toJson());

    convertEvent(event);

    if (EVENTS.contains(event.getState())) {

      logger.debug(event.toJson());

      IncidentEntity incidentEntity = new IncidentEntity();

      incidentEntity.setId(IdUtil.createId(event.getMetadata().getKey(), event.getMetadata().getPartitionId()));
      incidentEntity.setKey(event.getMetadata().getKey());
      incidentEntity.setPartitionId(event.getMetadata().getPartitionId());
      incidentEntity.setErrorType(event.getErrorType());
      incidentEntity.setErrorMessage(event.getErrorMessage());
      incidentEntity.setActivityId(event.getActivityId());
      if (event.getActivityInstanceKey() != null) {
        incidentEntity.setActivityInstanceId(IdUtil.createId(event.getActivityInstanceKey(), event.getMetadata().getPartitionId()));
      }
      if (event.getJobKey() != null) {
        incidentEntity.setJobId(String.valueOf(event.getJobKey()));
      }
      if (event.getWorkflowInstanceKey() != null) {
        incidentEntity.setWorkflowInstanceId(IdUtil.createId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId()));
      }

      org.camunda.operate.entities.IncidentState incidentState = fromZeebeIncidentState(event.getState());

      incidentEntity.setState(incidentState);

      updateMetadataFields(incidentEntity, event);

      //TODO will wait till capacity available, can throw InterruptedException
      entityStorage.getOperateEntitiesQueue(event.getMetadata().getTopicName()).put(incidentEntity);
    }
  }

  private void updateMetadataFields(IncidentEntity operateEntity, Record zeebeRecord) {
    RecordMetadata metadata = zeebeRecord.getMetadata();
    operateEntity.setPosition(metadata.getPosition());
  }

  private void convertEvent(IncidentEvent event) throws InterruptedException {
    EventEntity eventEntity = new EventEntity();

    loadEventGeneralData(event, eventEntity);

    if (event.getWorkflowInstanceKey() != null) {
      eventEntity.setWorkflowInstanceId(IdUtil.createId(event.getWorkflowInstanceKey(), event.getMetadata().getPartitionId()));
    }
    eventEntity.setBpmnProcessId(event.getBpmnProcessId());
    eventEntity.setActivityId(event.getActivityId());
    if (event.getActivityInstanceKey() != null) {
      eventEntity.setActivityInstanceId(IdUtil.createId(event.getActivityInstanceKey(), event.getMetadata().getPartitionId()));
    }

    EventMetadataEntity eventMetadata = new EventMetadataEntity();
    eventMetadata.setIncidentErrorMessage(event.getErrorMessage());
    eventMetadata.setIncidentErrorType(event.getErrorType());
    if (event.getJobKey() != null) {
      eventMetadata.setJobId(String.valueOf(event.getJobKey()));
    }
    eventEntity.setMetadata(eventMetadata);

    RecordMetadata metadata = event.getMetadata();
    String topicName = metadata.getTopicName();

    // TODO will wait till capacity available, can throw InterruptedException
    entityStorage.getOperateEntitiesQueue(topicName).put(eventEntity);
  }
}

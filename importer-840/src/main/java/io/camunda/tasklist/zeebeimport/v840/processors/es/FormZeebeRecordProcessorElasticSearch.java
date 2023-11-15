/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.zeebeimport.v840.processors.es;

import static io.camunda.tasklist.util.ElasticsearchUtil.UPDATE_RETRY_COUNT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.entities.FormEntity;
import io.camunda.tasklist.exceptions.PersistenceException;
import io.camunda.tasklist.schema.indices.FormIndex;
import io.camunda.tasklist.util.ConversionUtils;
import io.camunda.tasklist.zeebeimport.v840.record.value.deployment.FormRecordImpl;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.FormIntent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FormZeebeRecordProcessorElasticSearch {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FormZeebeRecordProcessorElasticSearch.class);

  @Autowired private ObjectMapper objectMapper;

  @Autowired private FormIndex formIndex;

  public void processFormRecord(Record record, BulkRequest bulkRequest)
      throws PersistenceException {

    final FormRecordImpl recordValue = (FormRecordImpl) record.getValue();

    if (record.getIntent().name().equals(FormIntent.CREATED.name())) {
      persistForm(
          recordValue.getFormKey(),
          bytesToXml(recordValue.getResource()),
          (long) recordValue.getVersion(),
          recordValue.getTenantId(),
          recordValue.getFormId(),
          false,
          bulkRequest);
    } else if (record.getIntent().name().equals(FormIntent.DELETED.name())) {
      persistForm(
          recordValue.getFormKey(),
          bytesToXml(recordValue.getResource()),
          (long) recordValue.getVersion(),
          recordValue.getTenantId(),
          recordValue.getFormId(),
          true,
          bulkRequest);
    } else {
      LOGGER.info("Form intent {} not supported", record.getIntent().name());
    }
  }

  private void persistForm(
      Long formKey,
      String schema,
      Long version,
      String tenantId,
      String formId,
      boolean isDelete,
      BulkRequest bulkRequest)
      throws PersistenceException {
    final FormEntity formEntity =
        new FormEntity(null, formId, schema, version, tenantId, formKey.toString(), false, false);

    try {
      if (isDelete) {
        // Delete operation
        bulkRequest.add(
            new UpdateRequest()
                .index(formIndex.getFullQualifiedName())
                .id(formEntity.getId())
                .upsert(objectMapper.writeValueAsString(formEntity), XContentType.JSON)
                .doc(Map.of(FormIndex.IS_DELETED, true))
                .retryOnConflict(UPDATE_RETRY_COUNT));
      } else {
        // Create operation
        bulkRequest.add(
            new IndexRequest()
                .index(formIndex.getFullQualifiedName())
                .id(ConversionUtils.toStringOrNull(formEntity.getId()))
                .source(objectMapper.writeValueAsString(formEntity), XContentType.JSON));
      }
    } catch (JsonProcessingException e) {
      throw new PersistenceException(
          String.format("Error preparing the form query for the formId: [%s]", formEntity.getId()),
          e);
    }
  }

  public static String bytesToXml(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }
}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.OptimizeDto;
import org.camunda.optimize.dto.optimize.importing.index.AllEntitiesBasedImportIndexDto;
import org.camunda.optimize.dto.optimize.importing.index.ImportIndexDto;
import org.camunda.optimize.dto.optimize.importing.index.TimestampBasedImportIndexDto;
import org.camunda.optimize.service.es.OptimizeElasticsearchClient;
import org.camunda.optimize.service.es.schema.index.index.ImportIndexIndex;
import org.camunda.optimize.service.util.EsHelper;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.IMPORT_INDEX_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.TIMESTAMP_BASED_IMPORT_INDEX_NAME;

@AllArgsConstructor
@Component
@Slf4j
public class ImportIndexWriter {

  private final OptimizeElasticsearchClient esClient;
  private final ObjectMapper objectMapper;
  private final DateTimeFormatter dateTimeFormatter;

  public void importIndexes(List<ImportIndexDto> importIndexDtos) {
    String importItemName = "import index information";
    log.debug("Writing [{}] {} to ES.", importIndexDtos.size(), importItemName);

    ElasticsearchWriterUtil.doBulkRequestWithList(
      esClient,
      importItemName,
      importIndexDtos,
      this::addImportIndexRequest
    );
  }

  private void addImportIndexRequest(BulkRequest bulkRequest, OptimizeDto optimizeDto) {
    if (optimizeDto instanceof TimestampBasedImportIndexDto) {
      TimestampBasedImportIndexDto timestampBasedIndexDto = (TimestampBasedImportIndexDto) optimizeDto;
      bulkRequest.add(createDefinitionBasedRequest(timestampBasedIndexDto));
    } else if (optimizeDto instanceof AllEntitiesBasedImportIndexDto) {
      AllEntitiesBasedImportIndexDto entitiesBasedIndexDto = (AllEntitiesBasedImportIndexDto) optimizeDto;
      bulkRequest.add(createAllEntitiesBasedRequest(entitiesBasedIndexDto));
    }
  }

  private IndexRequest createDefinitionBasedRequest(TimestampBasedImportIndexDto importIndex) {
    String currentTimeStamp = dateTimeFormatter.format(importIndex.getTimestampOfLastEntity());
    log.debug(
      "Writing definition based import index [{}] of type [{}] to elasticsearch",
      currentTimeStamp, importIndex.getEsTypeIndexRefersTo()
    );
    try {
      return new IndexRequest(TIMESTAMP_BASED_IMPORT_INDEX_NAME)
        .id(getId(importIndex))
        .source(objectMapper.writeValueAsString(importIndex), XContentType.JSON);
    } catch (JsonProcessingException e) {
      log.error("Was not able to write definition based import index of type [{}] to Elasticsearch. Reason: {}",
                importIndex.getEsTypeIndexRefersTo(), e
      );
      return new IndexRequest();
    }
  }

  private String getId(ImportIndexDto importIndex) {
    return EsHelper.constructKey(importIndex.getEsTypeIndexRefersTo(), importIndex.getEngine());
  }

  private IndexRequest createAllEntitiesBasedRequest(AllEntitiesBasedImportIndexDto importIndex) {
    log.debug("Writing all entities based import index type [{}] to elasticsearch. " +
                "Starting from [{}]",
              importIndex.getEsTypeIndexRefersTo(), importIndex.getImportIndex()
    );
    try {
      XContentBuilder sourceToAdjust = XContentFactory.jsonBuilder()
        .startObject()
        .field(ImportIndexIndex.ENGINE, importIndex.getEngine())
        .field(ImportIndexIndex.IMPORT_INDEX, importIndex.getImportIndex())
        .endObject();
      return new IndexRequest(IMPORT_INDEX_INDEX_NAME)
        .id(getId(importIndex))
        .source(sourceToAdjust);
    } catch (IOException e) {
      log.error(
        "Was not able to write all entities based import index of type [{}] to Elasticsearch. Reason: {}",
        importIndex.getEsTypeIndexRefersTo(), e
      );
      return new IndexRequest();
    }
  }

}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.webapp.opensearch.reader;

import static io.camunda.operate.store.opensearch.dsl.QueryDSL.*;
import static io.camunda.operate.store.opensearch.dsl.RequestDSL.searchRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.conditions.OpensearchCondition;
import io.camunda.operate.entities.JobEntity;
import io.camunda.operate.entities.ListenerType;
import io.camunda.operate.schema.templates.JobTemplate;
import io.camunda.operate.store.opensearch.client.sync.RichOpenSearchClient;
import io.camunda.operate.util.CollectionUtil;
import io.camunda.operate.webapp.reader.ListenerReader;
import io.camunda.operate.webapp.rest.dto.ListenerDto;
import io.camunda.operate.webapp.rest.dto.ListenerRequestDto;
import io.camunda.operate.webapp.rest.dto.ListenerResponseDto;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.SearchResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(OpensearchCondition.class)
@Component
public class OpensearchListenerReader extends OpensearchAbstractReader implements ListenerReader {

  private final JobTemplate jobTemplate;
  private final RichOpenSearchClient richOpenSearchClient;

  private final ObjectMapper objectMapper;

  public OpensearchListenerReader(
      final JobTemplate jobTemplate,
      final RichOpenSearchClient richOpenSearchClient,
      @Qualifier("operateObjectMapper") final ObjectMapper objectMapper) {
    this.jobTemplate = jobTemplate;
    this.richOpenSearchClient = richOpenSearchClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public ListenerResponseDto getListenerExecutions(
      final String processInstanceId, final ListenerRequestDto request) {
    final Query query =
        and(
            term(JobTemplate.PROCESS_INSTANCE_KEY, processInstanceId),
            term(JobTemplate.FLOW_NODE_ID, request.getFlowNodeId()),
            or(
                term(JobTemplate.JOB_KIND, ListenerType.EXECUTION_LISTENER.name()),
                term(JobTemplate.JOB_KIND, ListenerType.TASK_LISTENER.name())));

    final var searchRequestBuilder = searchRequestBuilder(jobTemplate.getAlias()).query(query);
    applySorting(searchRequestBuilder, request);
    searchRequestBuilder.size(request.getPageSize());

    final SearchResult<JobEntity> searchResult =
        richOpenSearchClient.doc().fixedSearch(searchRequestBuilder.build(), JobEntity.class);
    final Long totalHitCount = searchResult.hits().total().value();
    final List<ListenerDto> listeners =
        searchResult.hits().hits().stream()
            .map(
                hit -> {
                  final JobEntity entity = hit.source();
                  return ListenerDto.fromJobEntity(entity).setSortValues(hit.sort().toArray());
                })
            .collect(Collectors.toList());
    if (request.getSearchBefore() != null) {
      Collections.reverse(listeners);
    }
    return new ListenerResponseDto(listeners, totalHitCount);
  }

  private void applySorting(
      final SearchRequest.Builder searchSourceBuilder, final ListenerRequestDto request) {

    SortOrder sortOrder = SortOrder.Desc;

    if (request.getSorting() != null) {
      if (request.getSorting().getSortOrder() != null) {
        sortOrder =
            "asc".equals(request.getSorting().getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;
      }
    }

    final String missing;
    Object[] querySearchAfter = null;
    if (request.getSearchBefore() != null) {
      sortOrder = reverseOrder(sortOrder);
      missing = "_last";
      querySearchAfter = request.getSearchBefore(objectMapper);
    } else {
      missing = "_first";
      if (request.getSearchAfter() != null) {
        querySearchAfter = request.getSearchAfter(objectMapper);
      }
    }

    if (querySearchAfter != null) {
      searchSourceBuilder.searchAfter(CollectionUtil.toSafeListOfStrings(querySearchAfter));
    }

    searchSourceBuilder
        .sort(sortOptions(JobTemplate.TIME, sortOrder, missing))
        .sort(sortOptions(JobTemplate.JOB_KEY, sortOrder));
  }
}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.filter;

import org.apache.lucene.search.join.ScoreMode;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.flownode.FlowNodeDateFilterDataDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.camunda.optimize.service.es.filter.util.ModelElementFilterQueryUtil.createFlowNodeEndDateFilterQuery;
import static org.camunda.optimize.service.es.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

@Component
public class FlowNodeEndDateQueryFilter implements QueryFilter<FlowNodeDateFilterDataDto<?>> {
  @Override
  public void addFilters(final BoolQueryBuilder query, final List<FlowNodeDateFilterDataDto<?>> flowNodeEndDateFilters,
                         final FilterContext filterContext) {
    final List<QueryBuilder> filters = query.filter();
    for (FlowNodeDateFilterDataDto<?> flowNodeEndDateFilter : flowNodeEndDateFilters) {
      filters.add(
        nestedQuery(
          FLOW_NODE_INSTANCES,
          createFlowNodeEndDateFilterQuery(flowNodeEndDateFilter, filterContext.getTimezone()),
          ScoreMode.None
        )
      );
    }
  }
}

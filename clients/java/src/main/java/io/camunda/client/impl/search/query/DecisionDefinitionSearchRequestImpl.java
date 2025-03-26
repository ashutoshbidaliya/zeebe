/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.client.impl.search.query;

import static io.camunda.client.api.search.SearchRequestBuilders.decisionDefinitionFilter;
import static io.camunda.client.api.search.SearchRequestBuilders.decisionDefinitionSort;
import static io.camunda.client.api.search.SearchRequestBuilders.searchRequestPage;

import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.api.search.SearchRequestPage;
import io.camunda.client.api.search.filter.DecisionDefinitionFilter;
import io.camunda.client.api.search.request.DecisionDefinitionSearchRequest;
import io.camunda.client.api.search.request.FinalSearchRequestStep;
import io.camunda.client.api.search.response.DecisionDefinition;
import io.camunda.client.api.search.response.SearchQueryResponse;
import io.camunda.client.api.search.sort.DecisionDefinitionSort;
import io.camunda.client.impl.http.HttpCamundaFuture;
import io.camunda.client.impl.http.HttpClient;
import io.camunda.client.impl.search.SearchQuerySortRequest;
import io.camunda.client.impl.search.SearchQuerySortRequestMapper;
import io.camunda.client.impl.search.SearchResponseMapper;
import io.camunda.client.impl.search.TypedSearchRequestPropertyProvider;
import io.camunda.client.protocol.rest.DecisionDefinitionSearchQueryResult;
import io.camunda.client.protocol.rest.SearchQueryPageRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.hc.client5.http.config.RequestConfig;

public class DecisionDefinitionSearchRequestImpl
    extends TypedSearchRequestPropertyProvider<
        io.camunda.client.protocol.rest.DecisionDefinitionSearchQuery>
    implements DecisionDefinitionSearchRequest {

  private final io.camunda.client.protocol.rest.DecisionDefinitionSearchQuery request;
  private final JsonMapper jsonMapper;
  private final HttpClient httpClient;
  private final RequestConfig.Builder httpRequestConfig;

  public DecisionDefinitionSearchRequestImpl(
      final HttpClient httpClient, final JsonMapper jsonMapper) {
    request = new io.camunda.client.protocol.rest.DecisionDefinitionSearchQuery();
    this.jsonMapper = jsonMapper;
    this.httpClient = httpClient;
    httpRequestConfig = httpClient.newRequestConfig();
  }

  @Override
  public DecisionDefinitionSearchRequest filter(final DecisionDefinitionFilter value) {
    final io.camunda.client.protocol.rest.DecisionDefinitionFilter filter =
        provideSearchRequestProperty(value);
    request.setFilter(filter);
    return this;
  }

  @Override
  public DecisionDefinitionSearchRequest filter(final Consumer<DecisionDefinitionFilter> fn) {
    return filter(decisionDefinitionFilter(fn));
  }

  @Override
  public DecisionDefinitionSearchRequest sort(final DecisionDefinitionSort value) {
    final List<SearchQuerySortRequest> sorting = provideSearchRequestProperty(value);
    request.setSort(
        SearchQuerySortRequestMapper.toDecisionDefinitionSearchQuerySortRequest(sorting));
    return this;
  }

  @Override
  public DecisionDefinitionSearchRequest sort(final Consumer<DecisionDefinitionSort> fn) {
    return sort(decisionDefinitionSort(fn));
  }

  @Override
  public DecisionDefinitionSearchRequest page(final SearchRequestPage value) {
    final SearchQueryPageRequest page = provideSearchRequestProperty(value);
    request.setPage(page);
    return this;
  }

  @Override
  public DecisionDefinitionSearchRequest page(final Consumer<SearchRequestPage> fn) {
    return page(searchRequestPage(fn));
  }

  @Override
  protected io.camunda.client.protocol.rest.DecisionDefinitionSearchQuery
      getSearchRequestProperty() {
    return request;
  }

  @Override
  public FinalSearchRequestStep<DecisionDefinition> requestTimeout(final Duration requestTimeout) {
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public CamundaFuture<SearchQueryResponse<DecisionDefinition>> send() {
    final HttpCamundaFuture<SearchQueryResponse<DecisionDefinition>> result =
        new HttpCamundaFuture<>();
    httpClient.post(
        "/decision-definitions/search",
        jsonMapper.toJson(request),
        httpRequestConfig.build(),
        DecisionDefinitionSearchQueryResult.class,
        SearchResponseMapper::toDecisionDefinitionSearchResponse,
        result);
    return result;
  }
}

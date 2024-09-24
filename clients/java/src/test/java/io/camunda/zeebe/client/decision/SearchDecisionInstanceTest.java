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
package io.camunda.zeebe.client.decision;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.protocol.rest.DecisionInstanceSearchQueryRequest;
import io.camunda.zeebe.client.protocol.rest.DecisionInstanceStateEnum;
import io.camunda.zeebe.client.protocol.rest.DecisionInstanceTypeEnum;
import io.camunda.zeebe.client.util.ClientRestTest;
import org.junit.jupiter.api.Test;

class SearchDecisionInstanceTest extends ClientRestTest {

  @Test
  void shouldSearchDecisionInstance() {
    // when
    client.newDecisionInstanceQuery().send().join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getFilter()).isNull();
  }

  @Test
  void shouldSearchDecisionInstanceWithFullFilters() {
    // when
    client
        .newDecisionInstanceQuery()
        .filter(
            f ->
                f.decisionInstanceKey(1L)
                    .state(DecisionInstanceStateEnum.FAILED)
                    .evaluationFailure("ef")
                    .decisionType(DecisionInstanceTypeEnum.DECISION_TABLE)
                    .processDefinitionKey(2L)
                    .processInstanceKey(3L)
                    .decisionKey(4L)
                    .dmnDecisionId("ddi")
                    .dmnDecisionName("ddm")
                    .decisionVersion(5)
                    .tenantId("t"))
        .send()
        .join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getFilter().getDecisionInstanceKey()).isEqualTo(1L);
    assertThat(request.getFilter().getState()).isEqualTo(DecisionInstanceStateEnum.FAILED);
    assertThat(request.getFilter().getEvaluationFailure()).isEqualTo("ef");
    assertThat(request.getFilter().getDecisionType())
        .isEqualTo(DecisionInstanceTypeEnum.DECISION_TABLE);
    assertThat(request.getFilter().getProcessDefinitionKey()).isEqualTo(2L);
    assertThat(request.getFilter().getProcessInstanceKey()).isEqualTo(3L);
    assertThat(request.getFilter().getDecisionKey()).isEqualTo(4L);
    assertThat(request.getFilter().getDmnDecisionId()).isEqualTo("ddi");
    assertThat(request.getFilter().getDmnDecisionName()).isEqualTo("ddm");
    assertThat(request.getFilter().getDecisionVersion()).isEqualTo(5);
    assertThat(request.getFilter().getTenantId()).isEqualTo("t");
  }

  @Test
  void shouldSearchDecisionInstanceWithFullSorting() {
    // when
    client
        .newDecisionInstanceQuery()
        .sort(
            s ->
                s.decisionKey()
                    .asc()
                    .dmnDecisionId()
                    .asc()
                    .dmnDecisionName()
                    .desc()
                    .processInstanceId()
                    .asc()
                    .evaluationDate()
                    .asc()
                    .evaluationFailure()
                    .asc()
                    .decisionVersion()
                    .asc()
                    .state()
                    .asc()
                    .processDefinitionKey()
                    .asc()
                    .processInstanceId()
                    .asc()
                    .decisionType()
                    .asc()
                    .tenantId()
                    .asc())
        .send()
        .join();

    // then
    final DecisionInstanceSearchQueryRequest request =
        gatewayService.getLastRequest(DecisionInstanceSearchQueryRequest.class);
    assertThat(request.getSort().size()).isEqualTo(12);
    assertThat(request.getSort().get(0).getField()).isEqualTo("decisionKey");
    assertThat(request.getSort().get(0).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(1).getField()).isEqualTo("dmnDecisionId");
    assertThat(request.getSort().get(1).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(2).getField()).isEqualTo("dmnDecisionName");
    assertThat(request.getSort().get(2).getOrder()).isEqualTo("desc");
    assertThat(request.getSort().get(3).getField()).isEqualTo("processInstanceId");
    assertThat(request.getSort().get(3).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(4).getField()).isEqualTo("evaluationDate");
    assertThat(request.getSort().get(4).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(5).getField()).isEqualTo("evaluationFailure");
    assertThat(request.getSort().get(5).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(6).getField()).isEqualTo("decisionVersion");
    assertThat(request.getSort().get(6).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(7).getField()).isEqualTo("state");
    assertThat(request.getSort().get(7).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(8).getField()).isEqualTo("processDefinitionKey");
    assertThat(request.getSort().get(8).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(9).getField()).isEqualTo("processInstanceId");
    assertThat(request.getSort().get(9).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(10).getField()).isEqualTo("decisionType");
    assertThat(request.getSort().get(10).getOrder()).isEqualTo("asc");
    assertThat(request.getSort().get(11).getField()).isEqualTo("tenantId");
    assertThat(request.getSort().get(11).getOrder()).isEqualTo("asc");
  }
}

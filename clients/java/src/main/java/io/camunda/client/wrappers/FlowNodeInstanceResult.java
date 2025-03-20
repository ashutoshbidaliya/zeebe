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
package io.camunda.client.wrappers;

import io.camunda.client.impl.util.EnumUtil;

public class FlowNodeInstanceResult {

  public enum State {
    ACTIVE,
    COMPLETED,
    TERMINATED,
    UNKNOWN_ENUM_VALUE;

    public static io.camunda.client.protocol.rest.FlowNodeInstanceResult.StateEnum toProtocolEnum(
        final State value) {
      return (value == null)
          ? null
          : io.camunda.client.protocol.rest.FlowNodeInstanceResult.StateEnum.fromValue(
              value.name());
    }

    public static State fromProtocolEnum(
        final io.camunda.client.protocol.rest.FlowNodeInstanceResult.StateEnum value) {
      if (value == null) {
        return null;
      }
      try {
        return State.valueOf(value.name());
      } catch (final IllegalArgumentException e) {
        EnumUtil.logUnknownEnumValue(value, value.getClass().getName(), values());
        return UNKNOWN_ENUM_VALUE;
      }
    }
  }

  public enum Type {
    UNSPECIFIED,
    PROCESS,
    SUB_PROCESS,
    EVENT_SUB_PROCESS,
    AD_HOC_SUB_PROCESS,
    START_EVENT,
    INTERMEDIATE_CATCH_EVENT,
    INTERMEDIATE_THROW_EVENT,
    BOUNDARY_EVENT,
    END_EVENT,
    SERVICE_TASK,
    RECEIVE_TASK,
    USER_TASK,
    MANUAL_TASK,
    TASK,
    EXCLUSIVE_GATEWAY,
    INCLUSIVE_GATEWAY,
    PARALLEL_GATEWAY,
    EVENT_BASED_GATEWAY,
    SEQUENCE_FLOW,
    MULTI_INSTANCE_BODY,
    CALL_ACTIVITY,
    BUSINESS_RULE_TASK,
    SCRIPT_TASK,
    SEND_TASK,
    UNKNOWN,
    UNKNOWN_ENUM_VALUE;

    public static io.camunda.client.protocol.rest.FlowNodeInstanceResult.TypeEnum toProtocolEnum(
        final Type value) {
      return (value == null)
          ? null
          : io.camunda.client.protocol.rest.FlowNodeInstanceResult.TypeEnum.fromValue(value.name());
    }

    public static Type fromProtocolEnum(
        final io.camunda.client.protocol.rest.FlowNodeInstanceResult.TypeEnum value) {
      if (value == null) {
        return null;
      }
      try {
        return Type.valueOf(value.name());
      } catch (final IllegalArgumentException e) {
        EnumUtil.logUnknownEnumValue(value, value.getClass().getName(), values());
        return UNKNOWN_ENUM_VALUE;
      }
    }
  }
}

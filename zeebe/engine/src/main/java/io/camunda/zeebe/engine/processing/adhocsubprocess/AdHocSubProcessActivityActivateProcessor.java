/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.adhocsubprocess;

import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableAdHocSubProcess;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior.AuthorizationRequest;
import io.camunda.zeebe.engine.processing.identity.AuthorizationCheckBehavior.ForbiddenException;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessor;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.StateWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedCommandWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedRejectionWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.TypedResponseWriter;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.Writers;
import io.camunda.zeebe.engine.state.immutable.ElementInstanceState;
import io.camunda.zeebe.engine.state.immutable.ProcessState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.engine.state.instance.ElementInstance;
import io.camunda.zeebe.protocol.impl.record.value.adhocsubprocess.AdHocSubProcessActivityActivationElement;
import io.camunda.zeebe.protocol.impl.record.value.adhocsubprocess.AdHocSubProcessActivityActivationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.AdHocSubProcessActivityActivationIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.AdHocSubProcessActivityActivationRecordValue.AdHocSubProcessActivityActivationElementValue;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.stream.api.records.TypedRecord;
import io.camunda.zeebe.stream.api.state.KeyGenerator;
import java.util.List;

public class AdHocSubProcessActivityActivateProcessor
    implements TypedRecordProcessor<AdHocSubProcessActivityActivationRecord> {

  private static final String ERROR_MSG_ADHOC_SUBPROCESS_NOT_FOUND =
      "Expected to activate activities for ad-hoc subprocess but no ad-hoc subprocess instance found with key '%s'.";
  private static final String ERROR_MSG_DUPLICATE_ACTIVITIES =
      "Expected to activate activities for ad-hoc subprocess '%s', but duplicate activities were given.";
  private static final String ERROR_MSG_ADHOC_SUBPROCESS_IS_FINAL =
      "Expected to activate activities for ad-hoc subprocess '%s', but it is either completed or terminated.";
  private static final String ERROR_MSG_ADHOC_SUBPROCESS_IS_NOT_ACTIVE =
      "Expected to activate activities for ad-hoc subprocess '%s', but it is not active.";

  private final StateWriter stateWriter;
  private final TypedResponseWriter responseWriter;
  private final TypedRejectionWriter rejectionWriter;
  private final TypedCommandWriter commandWriter;
  private final ElementInstanceState elementInstanceState;
  private final ProcessState processState;
  private final AuthorizationCheckBehavior authCheckBehavior;
  private final KeyGenerator keyGenerator;

  public AdHocSubProcessActivityActivateProcessor(
      final Writers writers,
      final ProcessingState processingState,
      final AuthorizationCheckBehavior authCheckBehavior,
      final KeyGenerator keyGenerator) {
    stateWriter = writers.state();
    responseWriter = writers.response();
    rejectionWriter = writers.rejection();
    commandWriter = writers.command();
    processState = processingState.getProcessState();
    elementInstanceState = processingState.getElementInstanceState();
    this.authCheckBehavior = authCheckBehavior;
    this.keyGenerator = keyGenerator;
  }

  @Override
  public void processRecord(final TypedRecord<AdHocSubProcessActivityActivationRecord> command) {
    final var adHocSubprocessElementInstance =
        elementInstanceState.getInstance(
            Long.parseLong(command.getValue().getAdHocSubProcessInstanceKey()));
    if (adHocSubprocessElementInstance == null) {
      writeRejectionError(
          command,
          RejectionType.INVALID_STATE,
          String.format(
              ERROR_MSG_ADHOC_SUBPROCESS_NOT_FOUND,
              command.getValue().getAdHocSubProcessInstanceKey()));

      return;
    }

    authorize(command, adHocSubprocessElementInstance);

    if (hasDuplicateElements(command)) {
      writeRejectionError(
          command,
          RejectionType.INVALID_ARGUMENT,
          String.format(
              ERROR_MSG_DUPLICATE_ACTIVITIES, command.getValue().getAdHocSubProcessInstanceKey()));

      return;
    }

    final var adHocSubprocessElementId = adHocSubprocessElementInstance.getValue().getElementId();
    if (adHocSubprocessElementInstance.isInFinalState()) {
      writeRejectionError(
          command,
          RejectionType.INVALID_STATE,
          String.format(
              ERROR_MSG_ADHOC_SUBPROCESS_IS_FINAL,
              command.getValue().getAdHocSubProcessInstanceKey()));

      return;
    }

    if (!adHocSubprocessElementInstance.isActive()) {
      final var errorMessage =
          String.format(
              ERROR_MSG_ADHOC_SUBPROCESS_IS_NOT_ACTIVE,
              command.getValue().getAdHocSubProcessInstanceKey());
      rejectionWriter.appendRejection(command, RejectionType.INVALID_STATE, errorMessage);
      responseWriter.writeRejectionOnCommand(command, RejectionType.INVALID_STATE, errorMessage);

      return;
    }

    final var adHocSubprocessDefinition =
        processState
            .getProcessByKeyAndTenant(
                adHocSubprocessElementInstance.getValue().getProcessDefinitionKey(),
                adHocSubprocessElementInstance.getValue().getTenantId())
            .getProcess();

    final var adHocSubprocessElement =
        adHocSubprocessDefinition.getElementById(
            adHocSubprocessElementInstance.getValue().getElementId());
    final var adHocActivitiesById =
        ((ExecutableAdHocSubProcess) adHocSubprocessElement).getAdHocActivitiesById();

    // check that the given elements exist within the ad-hoc subprocess
    final var elementsNotInAdHocSubProcess =
        command.getValue().elements().stream()
            .map(AdHocSubProcessActivityActivationElement::getElementId)
            .filter(elementId -> !adHocActivitiesById.containsKey(elementId))
            .toList();
    if (!elementsNotInAdHocSubProcess.isEmpty()) {
      throw new ElementNotFoundException(adHocSubprocessElementId, elementsNotInAdHocSubProcess);
    }

    // activate the elements
    for (final var elementValue : command.getValue().getElements()) {
      final var elementToActivate =
          adHocSubprocessDefinition.getElementById(elementValue.getElementId());
      final var elementProcessInstanceRecord = new ProcessInstanceRecord();
      elementProcessInstanceRecord.wrap(adHocSubprocessElementInstance.getValue());
      elementProcessInstanceRecord
          .setFlowScopeKey(adHocSubprocessElementInstance.getKey())
          .setElementId(elementToActivate.getId())
          .setBpmnElementType(elementToActivate.getElementType())
          .setBpmnEventType(elementToActivate.getEventType());

      final long elementToActivateInstanceKey = keyGenerator.nextKey();
      commandWriter.appendFollowUpCommand(
          elementToActivateInstanceKey,
          ProcessInstanceIntent.ACTIVATE_ELEMENT,
          elementProcessInstanceRecord);
    }

    stateWriter.appendFollowUpEvent(
        command.getKey(), AdHocSubProcessActivityActivationIntent.ACTIVATED, command.getValue());

    responseWriter.writeEventOnCommand(
        command.getKey(),
        AdHocSubProcessActivityActivationIntent.ACTIVATED,
        command.getValue(),
        command);
  }

  @Override
  public ProcessingError tryHandleError(
      final TypedRecord<AdHocSubProcessActivityActivationRecord> command, final Throwable error) {
    return switch (error) {
      case final IllegalArgumentException e -> {
        writeRejectionError(command, RejectionType.INVALID_ARGUMENT, e.getMessage());
        yield ProcessingError.EXPECTED_ERROR;
      }
      case final IllegalStateException e -> {
        writeRejectionError(command, RejectionType.INVALID_STATE, e.getMessage());
        yield ProcessingError.EXPECTED_ERROR;
      }
      case final ForbiddenException e -> {
        writeRejectionError(command, e.getRejectionType(), e.getMessage());
        yield ProcessingError.EXPECTED_ERROR;
      }
      default -> ProcessingError.UNEXPECTED_ERROR;
    };
  }

  private void writeRejectionError(
      final TypedRecord<AdHocSubProcessActivityActivationRecord> command,
      final RejectionType rejectionType,
      final String errorMessage) {
    rejectionWriter.appendRejection(command, rejectionType, errorMessage);
    responseWriter.writeRejectionOnCommand(command, rejectionType, errorMessage);
  }

  private boolean hasDuplicateElements(
      final TypedRecord<AdHocSubProcessActivityActivationRecord> command) {
    return command.getValue().getElements().stream()
            .map(AdHocSubProcessActivityActivationElementValue::getElementId)
            .distinct()
            .count()
        != command.getValue().getElements().size();
  }

  private void authorize(
      final TypedRecord<AdHocSubProcessActivityActivationRecord> command,
      final ElementInstance adHocSubprocessElementInstance) {
    final var authRequest =
        new AuthorizationRequest(
                command,
                AuthorizationResourceType.PROCESS_DEFINITION,
                PermissionType.UPDATE_PROCESS_INSTANCE,
                adHocSubprocessElementInstance.getValue().getTenantId())
            .addResourceId(adHocSubprocessElementInstance.getValue().getBpmnProcessId());

    final var authResult = authCheckBehavior.isAuthorized(authRequest);
    if (authResult.isLeft()) {
      throw new ForbiddenException(authRequest);
    }
  }

  private static final class DuplicateElementsException extends IllegalArgumentException {
    private static final String ERROR_MESSAGE = "Duplicate elements %s not allowed.";

    private DuplicateElementsException(final List<String> elementIds) {
      super(String.format(ERROR_MESSAGE, elementIds));
    }
  }

  private static final class ElementNotFoundException extends IllegalStateException {
    private static final String ERROR_MESSAGE =
        "Elements %s do not exist in ad-hoc subprocess <%s>";

    private ElementNotFoundException(
        final String adHocSubprocessId, final List<String> elementIds) {
      super(String.format(ERROR_MESSAGE, elementIds, adHocSubprocessId));
    }
  }

  private static final class AdHocSubProcessInFinalStateException extends IllegalStateException {
    private static final String ERROR_MESSAGE =
        "Ad-hoc subprocess <%s> is already in a terminal state. Can't activate any activities.";

    private AdHocSubProcessInFinalStateException(final String adHocSubprocessElementId) {
      super(String.format(ERROR_MESSAGE, adHocSubprocessElementId));
    }
  }

  private static final class AdHocSubProcessInstanceIsNullException extends IllegalStateException {
    private static final String ERROR_MESSAGE = "Ad-hoc subprocess instance <%s> is null.";

    private AdHocSubProcessInstanceIsNullException(final String adHocSubprocessInstanceKey) {
      super(String.format(ERROR_MESSAGE, adHocSubprocessInstanceKey));
    }
  }

  private static final class AdHocSubProcessInstanceNotActiveException
      extends IllegalStateException {
    private static final String ERROR_MESSAGE = "Ad-hoc subprocess instance <%s> is not active.";

    private AdHocSubProcessInstanceNotActiveException(final String adHocSubprocessInstanceKey) {
      super(String.format(ERROR_MESSAGE, adHocSubprocessInstanceKey));
    }
  }
}

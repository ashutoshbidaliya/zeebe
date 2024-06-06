/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.transport.adminapi;

import io.camunda.zeebe.broker.transport.AsyncApiRequestHandler.ResponseWriter;
import io.camunda.zeebe.protocol.management.AdminResponseEncoder;
import io.camunda.zeebe.protocol.record.MessageHeaderEncoder;
import io.camunda.zeebe.transport.ServerOutput;
import io.camunda.zeebe.transport.impl.ServerResponseImpl;
import org.agrona.MutableDirectBuffer;

public final class ApiResponseWriter implements ResponseWriter {
  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final AdminResponseEncoder responseEncoder = new AdminResponseEncoder();
  private final ServerResponseImpl response = new ServerResponseImpl();
  private String payload = null;

  @Override
  public void tryWriteResponse(
      final ServerOutput output, final int partitionId, final long requestId) {
    try {
      response.reset().writer(this).setPartitionId(partitionId).setRequestId(requestId);
      output.sendResponse(response);
    } finally {
      reset();
    }
  }

  @Override
  public void reset() {}

  public AdminResponseEncoder getResponseEncoder() {
    return responseEncoder;
  }

  public void setPayload(final String payload) {
    this.payload = payload;
  }

  @Override
  public int getLength() {
    return MessageHeaderEncoder.ENCODED_LENGTH
        + AdminResponseEncoder.BLOCK_LENGTH
        + payload.getBytes().length
        + 4;
  }

  @Override
  public void write(final MutableDirectBuffer buffer, int offset) {
    headerEncoder.wrap(buffer, offset);

    headerEncoder
        .blockLength(responseEncoder.sbeBlockLength())
        .templateId(responseEncoder.sbeTemplateId())
        .schemaId(responseEncoder.sbeSchemaId())
        .version(responseEncoder.sbeSchemaVersion());

    offset += headerEncoder.encodedLength();

    responseEncoder.wrap(buffer, offset);
    if (payload != null) {
      responseEncoder.putPayload(payload.getBytes(), 0, payload.getBytes().length);
    }
  }
}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.upgrade.version30.indices;

import org.camunda.optimize.dto.optimize.TenantDto;
import org.camunda.optimize.service.es.schema.DefaultIndexMappingCreator;
import org.camunda.optimize.upgrade.es.ElasticsearchConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class TenantIndexV2 extends DefaultIndexMappingCreator {
  public static final int VERSION = 2;

  @Override
  public String getIndexName() {
    return ElasticsearchConstants.TENANT_INDEX_NAME;
  }

  @Override
  public int getVersion() {
    return VERSION;
  }

  @Override
  public XContentBuilder addProperties(XContentBuilder xContentBuilder) throws IOException {
    // @formatter:off
    return xContentBuilder
      .startObject(TenantDto.Fields.id.name())
      .field("type", "text")
      .field("index", false)
      .endObject()
      .startObject(TenantDto.Fields.name.name())
      .field("type", "text")
      .field("index", false)
      .endObject()
      .startObject(TenantDto.Fields.engine.name())
      .field("type", "text")
      .field("index", false)
      .endObject()
      ;
    // @formatter:on
  }

}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application.commons.migration;

import io.camunda.exporter.adapters.ClientAdapter;
import io.camunda.exporter.schema.SchemaManager;
import io.camunda.exporter.schema.config.SearchEngineConfiguration;
import io.camunda.search.connect.configuration.DatabaseType;
import io.camunda.webapps.schema.descriptors.IndexDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaManagerHelper {
  private static final Logger LOG = LoggerFactory.getLogger(SchemaManagerHelper.class);

  private SchemaManagerHelper() {}

  public static void createSchema(
      final SearchEngineConfiguration configuration, final ClientAdapter clientAdapter) {
    final var isElasticsearch = configuration.connect().getTypeEnum() == DatabaseType.ELASTICSEARCH;

    final IndexDescriptors indexDescriptors =
        new IndexDescriptors(configuration.index().getPrefix(), isElasticsearch);

    final SchemaManager schemaManager =
        new SchemaManager(
            clientAdapter.getSearchEngineClient(),
            indexDescriptors.indices(),
            indexDescriptors.templates(),
            configuration,
            clientAdapter.objectMapper());

    schemaManager.startup();
  }
}

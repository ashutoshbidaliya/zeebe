/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.indices;

import io.camunda.operate.conditions.DatabaseInfo;
import io.camunda.operate.schema.backup.Prio4Backup;
import io.camunda.webapps.schema.descriptors.operate.OperateIndexDescriptor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class MigrationRepositoryIndex extends OperateIndexDescriptor implements Prio4Backup {

  public static final String INDEX_NAME = "migration-steps-repository";

  public MigrationRepositoryIndex() {
    super("", false);
  }

  @PostConstruct
  public void init() {
    isElasticsearch = DatabaseInfo.isElasticsearch();
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return "1.1.0";
  }
}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.system.configuration.backup;

import io.camunda.zeebe.broker.system.configuration.ConfigurationEntry;
import java.util.Objects;

public class GCSBackupStoreConfig implements ConfigurationEntry {
  private String bucketName;
  private String basePath;

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(final String bucketName) {
    this.bucketName = bucketName;
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(final String basePath) {
    this.basePath = basePath;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final GCSBackupStoreConfig that = (GCSBackupStoreConfig) o;
    return Objects.equals(bucketName, that.bucketName) && Objects.equals(basePath, that.basePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucketName, basePath);
  }

  @Override
  public String toString() {
    return "GCSBackupStoreConfig{"
        + "bucketName='"
        + bucketName
        + '\''
        + ", basePath='"
        + basePath
        + '\''
        + '}';
  }
}

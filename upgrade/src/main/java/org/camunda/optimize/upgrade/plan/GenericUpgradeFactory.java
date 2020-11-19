/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.upgrade.plan;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.camunda.optimize.service.metadata.PreviousVersion;
import org.camunda.optimize.service.metadata.Version;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenericUpgradeFactory {

  public static UpgradePlan createUpgradePlan() {
    return UpgradePlanBuilder.createUpgradePlan()
      .fromVersion(PreviousVersion.PREVIOUS_VERSION)
      .toVersion(Version.VERSION)
      .build();
  }

}

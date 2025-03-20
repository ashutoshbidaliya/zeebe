/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.security.configuration.ConfiguredUser;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MappingIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationOwnerType;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.protocol.record.value.UserRecordValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.List;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class MappingUpdateAuthorizationTest {
  private static final ConfiguredUser DEFAULT_USER =
      new ConfiguredUser(
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString());

  @Rule
  public final EngineRule engine =
      EngineRule.singlePartition()
          .withIdentitySetup()
          .withSecurityConfig(cfg -> cfg.getAuthorizations().setEnabled(true))
          .withSecurityConfig(cfg -> cfg.getInitialization().setUsers(List.of(DEFAULT_USER)));

  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldBeAuthorizedToUpdateMappingWithDefaultUser() {
    // given
    final var claimName = UUID.randomUUID().toString();
    final var claimValue = UUID.randomUUID().toString();
    final var claimValueNew = UUID.randomUUID().toString();
    final var name = UUID.randomUUID().toString();
    final var id = UUID.randomUUID().toString();
    engine
        .mapping()
        .newMapping(claimName)
        .withClaimValue(claimValue)
        .withName(name)
        .withId(id)
        .create(DEFAULT_USER.getUsername());

    // when
    engine
        .mapping()
        .updateMapping(id)
        .withClaimValue(claimValueNew)
        .withName(name)
        .withClaimName(claimName)
        .update(DEFAULT_USER.getUsername());

    // then
    assertThat(
            RecordingExporter.mappingRecords(MappingIntent.UPDATED)
                .withClaimName(claimName)
                .withClaimValue(claimValueNew)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeAuthorizedToUpdateMappingWithPermissions() {
    // given
    final var claimName = UUID.randomUUID().toString();
    final var claimValue = UUID.randomUUID().toString();
    final var claimValueNew = UUID.randomUUID().toString();
    final var name = UUID.randomUUID().toString();
    final var id = UUID.randomUUID().toString();

    final var user = createUser();
    addPermissionsToUser(user, AuthorizationResourceType.MAPPING_RULE, PermissionType.UPDATE);

    engine
        .mapping()
        .newMapping(claimName)
        .withClaimValue(claimValue)
        .withName(name)
        .withId(id)
        .create(DEFAULT_USER.getUsername());

    // when
    engine
        .mapping()
        .updateMapping(id)
        .withClaimValue(claimValue)
        .withClaimValue(claimValueNew)
        .withName(name)
        .withClaimName(claimName)
        .update(user.getUsername());

    // then
    assertThat(
            RecordingExporter.mappingRecords(MappingIntent.UPDATED)
                .withClaimName(claimName)
                .withClaimValue(claimValueNew)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeUnAuthorizedToUpdateMappingWithoutPermissions() {
    // given
    final var claimName = UUID.randomUUID().toString();
    final var claimValue = UUID.randomUUID().toString();
    final var claimValueNew = UUID.randomUUID().toString();
    final var name = UUID.randomUUID().toString();
    final var id = UUID.randomUUID().toString();
    final var user = createUser();
    engine
        .mapping()
        .newMapping(claimName)
        .withClaimValue(claimValue)
        .withName(name)
        .withId(id)
        .create(DEFAULT_USER.getUsername());

    // when
    final var rejection =
        engine
            .mapping()
            .updateMapping(id)
            .withClaimValue(claimValue)
            .withClaimValue(claimValueNew)
            .withName(name)
            .withClaimName(claimName)
            .expectRejection()
            .update(user.getUsername());

    // then
    Assertions.assertThat(rejection)
        .hasRejectionType(RejectionType.FORBIDDEN)
        .hasRejectionReason(
            "Insufficient permissions to perform operation 'UPDATE' on resource 'MAPPING_RULE', required resource identifiers are one of '[*, "
                + id
                + "]'");
  }

  private UserRecordValue createUser() {
    return engine
        .user()
        .newUser(UUID.randomUUID().toString())
        .withPassword(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .withEmail(UUID.randomUUID().toString())
        .create()
        .getValue();
  }

  private void addPermissionsToUser(
      final UserRecordValue user,
      final AuthorizationResourceType authorization,
      final PermissionType permissionType) {
    engine
        .authorization()
        .newAuthorization()
        .withPermissions(permissionType)
        .withOwnerId(user.getUsername())
        .withOwnerType(AuthorizationOwnerType.USER)
        .withResourceType(authorization)
        .withResourceId("*")
        .create(DEFAULT_USER.getUsername());
  }
}

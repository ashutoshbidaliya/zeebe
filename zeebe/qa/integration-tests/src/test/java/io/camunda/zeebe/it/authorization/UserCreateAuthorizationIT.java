/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.authorization;

import static io.camunda.zeebe.it.util.AuthorizationsUtil.createClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.application.Profile;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.protocol.rest.PermissionTypeEnum;
import io.camunda.client.protocol.rest.ResourceTypeEnum;
import io.camunda.zeebe.it.util.AuthorizationsUtil;
import io.camunda.zeebe.it.util.AuthorizationsUtil.Permissions;
import io.camunda.zeebe.qa.util.cluster.TestStandaloneBroker;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import io.camunda.zeebe.test.util.testcontainers.TestSearchContainers;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoCloseResources
@Testcontainers
@ZeebeIntegration
public class UserCreateAuthorizationIT {
  @Container
  private static final ElasticsearchContainer CONTAINER =
      TestSearchContainers.createDefeaultElasticsearchContainer();

  private static AuthorizationsUtil authUtil;
  @AutoCloseResource private static CamundaClient defaultUserClient;

  @TestZeebe(autoStart = false)
  private TestStandaloneBroker broker =
      new TestStandaloneBroker()
          .withRecordingExporter(true)
          .withSecurityConfig(c -> c.getAuthorizations().setEnabled(true))
          .withAdditionalProfile(Profile.AUTH_BASIC);

  @BeforeEach
  void beforeEach() {
    broker.withCamundaExporter("http://" + CONTAINER.getHttpHostAddress());
    broker.start();

    final var defaultUsername = "demo";
    defaultUserClient = createClient(broker, defaultUsername, "demo");
    authUtil = new AuthorizationsUtil(broker, defaultUserClient, CONTAINER.getHttpHostAddress());

    authUtil.awaitUserExistsInElasticsearch(defaultUsername);
  }

  @Test
  void shouldBeAuthorizedToCreateUserWithDefaultUser() {
    // given
    final var username = UUID.randomUUID().toString();

    // when
    final var response =
        defaultUserClient
            .newUserCreateCommand()
            .username(username)
            .name("Foo")
            .email("bar@baz.com")
            .password("zabraboof")
            .send()
            .join();

    // then
    assertThat(response.getUserKey()).isPositive();
  }

  @Test
  void shouldBeAuthorizedToCreateUserWithPermissions() {
    // given
    final var authUsername = UUID.randomUUID().toString();
    final var newUsername = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUserWithPermissions(
        authUsername,
        password,
        new Permissions(ResourceTypeEnum.USER, PermissionTypeEnum.CREATE, List.of("*")));

    try (final var client = authUtil.createClient(authUsername, password)) {
      // when
      final var response =
          client
              .newUserCreateCommand()
              .username(newUsername)
              .name("Foo")
              .email("bar@baz.com")
              .password("zabraboof")
              .send()
              .join();

      // then
      assertThat(response.getUserKey()).isPositive();
    }
  }

  @Test
  void shouldBeUnAuthorizedToCreateUserWithoutPermissions() {
    // given
    final var authUsername = UUID.randomUUID().toString();
    final var newUsername = UUID.randomUUID().toString();
    final var password = "password";
    authUtil.createUser(authUsername, password);

    // when
    try (final var client = authUtil.createClient(authUsername, password)) {
      final var response =
          client
              .newUserCreateCommand()
              .username(newUsername)
              .name("Foo")
              .email("bar@baz.com")
              .password("zabraboof")
              .send();

      // then
      assertThatThrownBy(response::join)
          .isInstanceOf(ProblemException.class)
          .hasMessageContaining("title: FORBIDDEN")
          .hasMessageContaining("status: 403")
          .hasMessageContaining(
              "Insufficient permissions to perform operation 'CREATE' on resource 'USER'");
    }
  }
}

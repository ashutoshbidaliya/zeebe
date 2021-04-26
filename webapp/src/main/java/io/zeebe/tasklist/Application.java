/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist;

import io.zeebe.tasklist.data.DataGenerator;
import io.zeebe.tasklist.webapp.security.TasklistURIs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@ComponentScan(
    basePackages = "io.zeebe.tasklist",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.zeebe\\.tasklist\\.zeebeimport\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.zeebe\\.tasklist\\.webapp\\..*"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = "io\\.zeebe\\.tasklist\\.archiver\\..*")
    },
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableAutoConfiguration
public class Application {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    // To ensure that debug logging performed using java.util.logging is routed into Log4j 2
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    final SpringApplication springApplication = new SpringApplication(Application.class);
    // use fully qualified names as bean name, as we have classes with same names for different
    // versions of importer
    springApplication.setAddCommandLineProperties(true);
    setDefaultProperties(springApplication);
    setDefaultAuthProfile(springApplication);
    springApplication.run(args);
  }

  private static void setDefaultProperties(final SpringApplication springApplication) {
    final Map<String, Object> defaultProperties = new HashMap<>();
    defaultProperties.putAll(getManagementProperties());
    defaultProperties.putAll(getGraphqlProperties());
    springApplication.setDefaultProperties(defaultProperties);
  }

  private static void setDefaultAuthProfile(final SpringApplication springApplication) {
    springApplication.addInitializers(
        configurableApplicationContext -> {
          final ConfigurableEnvironment env = configurableApplicationContext.getEnvironment();
          final Set<String> activeProfiles = Set.of(env.getActiveProfiles());
          if (TasklistURIs.AUTH_PROFILES.stream().noneMatch(activeProfiles::contains)) {
            env.addActiveProfile(TasklistURIs.DEFAULT_AUTH);
          }
        });
  }

  private static Map<String, Object> getGraphqlProperties() {
    // GraphQL inspection tool is disabled by default
    // Exception handler is enabled
    return Map.of(
        "graphql.playground.enabled", "false",
        "graphql.servlet.exception-handlers-enabled", "true");
  }

  public static Map<String, Object> getManagementProperties() {
    return Map.of(
        // disable default health indicators:
        // https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-health-indicators
        "management.health.defaults.enabled", "false",

        // enable Kubernetes health groups:
        // https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready-kubernetes-probes
        "management.endpoint.health.probes.enabled", "true",

        // enable health check and metrics endpoints
        "management.endpoints.web.exposure.include", "health, prometheus, loggers",

        // add custom check to standard readiness check
        "management.endpoint.health.group.readiness.include", "readinessState,elsIndicesCheck");
  }

  @Bean(name = "dataGenerator")
  @ConditionalOnMissingBean
  public DataGenerator stubDataGenerator() {
    LOGGER.debug("Create Data generator stub");
    return DataGenerator.DO_NOTHING;
  }
}

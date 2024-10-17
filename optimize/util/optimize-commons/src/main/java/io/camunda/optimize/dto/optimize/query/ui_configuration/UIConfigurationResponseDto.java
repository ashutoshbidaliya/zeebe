/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.query.ui_configuration;

import io.camunda.optimize.service.util.configuration.DatabaseType;
import io.camunda.optimize.service.util.configuration.OptimizeProfile;
import java.util.List;
import java.util.Map;

public class UIConfigurationResponseDto {

  private boolean emailEnabled;
  private boolean sharingEnabled;
  private boolean tenantsAvailable;
  private boolean userSearchAvailable;
  private boolean userTaskAssigneeAnalyticsEnabled;
  private String optimizeVersion;
  private String optimizeDocsVersion;
  private boolean isEnterpriseMode;
  private OptimizeProfile optimizeProfile;
  private Map<String, WebappsEndpointDto> webappsEndpoints;
  private Map<AppName, String> webappsLinks; // links for the app switcher
  private String notificationsUrl;
  private List<String> webhooks;
  private boolean logoutHidden;
  private int maxNumDataSourcesForReport;
  private Integer exportCsvLimit;
  private DatabaseType optimizeDatabase;
  private boolean validLicense;
  private String licenseType;

  private MixpanelConfigResponseDto mixpanel = new MixpanelConfigResponseDto();

  private OnboardingResponseDto onboarding = new OnboardingResponseDto();

  public UIConfigurationResponseDto(
      boolean emailEnabled,
      boolean sharingEnabled,
      boolean tenantsAvailable,
      boolean userSearchAvailable,
      boolean userTaskAssigneeAnalyticsEnabled,
      String optimizeVersion,
      String optimizeDocsVersion,
      boolean isEnterpriseMode,
      OptimizeProfile optimizeProfile,
      Map<String, WebappsEndpointDto> webappsEndpoints,
      Map<AppName, String> webappsLinks,
      String notificationsUrl,
      List<String> webhooks,
      boolean logoutHidden,
      int maxNumDataSourcesForReport,
      Integer exportCsvLimit,
      DatabaseType optimizeDatabase,
      boolean validLicense,
      String licenseType,
      MixpanelConfigResponseDto mixpanel,
      OnboardingResponseDto onboarding) {
    this.emailEnabled = emailEnabled;
    this.sharingEnabled = sharingEnabled;
    this.tenantsAvailable = tenantsAvailable;
    this.userSearchAvailable = userSearchAvailable;
    this.userTaskAssigneeAnalyticsEnabled = userTaskAssigneeAnalyticsEnabled;
    this.optimizeVersion = optimizeVersion;
    this.optimizeDocsVersion = optimizeDocsVersion;
    this.isEnterpriseMode = isEnterpriseMode;
    this.optimizeProfile = optimizeProfile;
    this.webappsEndpoints = webappsEndpoints;
    this.webappsLinks = webappsLinks;
    this.notificationsUrl = notificationsUrl;
    this.webhooks = webhooks;
    this.logoutHidden = logoutHidden;
    this.maxNumDataSourcesForReport = maxNumDataSourcesForReport;
    this.exportCsvLimit = exportCsvLimit;
    this.optimizeDatabase = optimizeDatabase;
    this.validLicense = validLicense;
    this.licenseType = licenseType;
    this.mixpanel = mixpanel;
    this.onboarding = onboarding;
  }

  public UIConfigurationResponseDto() {}

  public boolean isEmailEnabled() {
    return this.emailEnabled;
  }

  public boolean isSharingEnabled() {
    return this.sharingEnabled;
  }

  public boolean isTenantsAvailable() {
    return this.tenantsAvailable;
  }

  public boolean isUserSearchAvailable() {
    return this.userSearchAvailable;
  }

  public boolean isUserTaskAssigneeAnalyticsEnabled() {
    return this.userTaskAssigneeAnalyticsEnabled;
  }

  public String getOptimizeVersion() {
    return this.optimizeVersion;
  }

  public String getOptimizeDocsVersion() {
    return this.optimizeDocsVersion;
  }

  public boolean isEnterpriseMode() {
    return this.isEnterpriseMode;
  }

  public OptimizeProfile getOptimizeProfile() {
    return this.optimizeProfile;
  }

  public Map<String, WebappsEndpointDto> getWebappsEndpoints() {
    return this.webappsEndpoints;
  }

  public Map<AppName, String> getWebappsLinks() {
    return this.webappsLinks;
  }

  public String getNotificationsUrl() {
    return this.notificationsUrl;
  }

  public List<String> getWebhooks() {
    return this.webhooks;
  }

  public boolean isLogoutHidden() {
    return this.logoutHidden;
  }

  public int getMaxNumDataSourcesForReport() {
    return this.maxNumDataSourcesForReport;
  }

  public Integer getExportCsvLimit() {
    return this.exportCsvLimit;
  }

  public DatabaseType getOptimizeDatabase() {
    return this.optimizeDatabase;
  }

  public boolean isValidLicense() {
    return this.validLicense;
  }

  public String getLicenseType() {
    return this.licenseType;
  }

  public MixpanelConfigResponseDto getMixpanel() {
    return this.mixpanel;
  }

  public OnboardingResponseDto getOnboarding() {
    return this.onboarding;
  }

  public void setEmailEnabled(boolean emailEnabled) {
    this.emailEnabled = emailEnabled;
  }

  public void setSharingEnabled(boolean sharingEnabled) {
    this.sharingEnabled = sharingEnabled;
  }

  public void setTenantsAvailable(boolean tenantsAvailable) {
    this.tenantsAvailable = tenantsAvailable;
  }

  public void setUserSearchAvailable(boolean userSearchAvailable) {
    this.userSearchAvailable = userSearchAvailable;
  }

  public void setUserTaskAssigneeAnalyticsEnabled(boolean userTaskAssigneeAnalyticsEnabled) {
    this.userTaskAssigneeAnalyticsEnabled = userTaskAssigneeAnalyticsEnabled;
  }

  public void setOptimizeVersion(String optimizeVersion) {
    this.optimizeVersion = optimizeVersion;
  }

  public void setOptimizeDocsVersion(String optimizeDocsVersion) {
    this.optimizeDocsVersion = optimizeDocsVersion;
  }

  public void setEnterpriseMode(boolean isEnterpriseMode) {
    this.isEnterpriseMode = isEnterpriseMode;
  }

  public void setOptimizeProfile(OptimizeProfile optimizeProfile) {
    this.optimizeProfile = optimizeProfile;
  }

  public void setWebappsEndpoints(Map<String, WebappsEndpointDto> webappsEndpoints) {
    this.webappsEndpoints = webappsEndpoints;
  }

  public void setWebappsLinks(Map<AppName, String> webappsLinks) {
    this.webappsLinks = webappsLinks;
  }

  public void setNotificationsUrl(String notificationsUrl) {
    this.notificationsUrl = notificationsUrl;
  }

  public void setWebhooks(List<String> webhooks) {
    this.webhooks = webhooks;
  }

  public void setLogoutHidden(boolean logoutHidden) {
    this.logoutHidden = logoutHidden;
  }

  public void setMaxNumDataSourcesForReport(int maxNumDataSourcesForReport) {
    this.maxNumDataSourcesForReport = maxNumDataSourcesForReport;
  }

  public void setExportCsvLimit(Integer exportCsvLimit) {
    this.exportCsvLimit = exportCsvLimit;
  }

  public void setOptimizeDatabase(DatabaseType optimizeDatabase) {
    this.optimizeDatabase = optimizeDatabase;
  }

  public void setValidLicense(boolean validLicense) {
    this.validLicense = validLicense;
  }

  public void setLicenseType(String licenseType) {
    this.licenseType = licenseType;
  }

  public void setMixpanel(MixpanelConfigResponseDto mixpanel) {
    this.mixpanel = mixpanel;
  }

  public void setOnboarding(OnboardingResponseDto onboarding) {
    this.onboarding = onboarding;
  }

  public boolean equals(final Object o) {
    return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
  }

  protected boolean canEqual(final Object other) {
    return other instanceof UIConfigurationResponseDto;
  }

  public int hashCode() {
    return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
  }

  public String toString() {
    return "UIConfigurationResponseDto(emailEnabled="
        + this.isEmailEnabled()
        + ", sharingEnabled="
        + this.isSharingEnabled()
        + ", tenantsAvailable="
        + this.isTenantsAvailable()
        + ", userSearchAvailable="
        + this.isUserSearchAvailable()
        + ", userTaskAssigneeAnalyticsEnabled="
        + this.isUserTaskAssigneeAnalyticsEnabled()
        + ", optimizeVersion="
        + this.getOptimizeVersion()
        + ", optimizeDocsVersion="
        + this.getOptimizeDocsVersion()
        + ", isEnterpriseMode="
        + this.isEnterpriseMode()
        + ", optimizeProfile="
        + this.getOptimizeProfile()
        + ", webappsEndpoints="
        + this.getWebappsEndpoints()
        + ", webappsLinks="
        + this.getWebappsLinks()
        + ", notificationsUrl="
        + this.getNotificationsUrl()
        + ", webhooks="
        + this.getWebhooks()
        + ", logoutHidden="
        + this.isLogoutHidden()
        + ", maxNumDataSourcesForReport="
        + this.getMaxNumDataSourcesForReport()
        + ", exportCsvLimit="
        + this.getExportCsvLimit()
        + ", optimizeDatabase="
        + this.getOptimizeDatabase()
        + ", validLicense="
        + this.isValidLicense()
        + ", licenseType="
        + this.getLicenseType()
        + ", mixpanel="
        + this.getMixpanel()
        + ", onboarding="
        + this.getOnboarding()
        + ")";
  }
}

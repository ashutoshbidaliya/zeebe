/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.webapp.security.sso.model;

import java.io.Serializable;

public class ClusterInfo implements Serializable {
  private String name;
  private SalesPlan salesPlan;
  private OrgPermissions permissions;

  public ClusterInfo() {}

  public ClusterInfo(String name, OrgPermissions permissions, SalesPlan salesPlan) {
    this.name = name;
    this.permissions = permissions;
    this.salesPlan = salesPlan;
  }

  public String getName() {
    return name;
  }

  public OrgPermissions getPermissions() {
    return permissions;
  }

  public SalesPlan getSalesPlan() {
    return salesPlan;
  }

  public static class SalesPlan {
    private String type;

    public SalesPlan() {}

    public SalesPlan(final String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  public static class OrgPermissions {
    private OrgPermissions cluster;
    private Permission tasklist;

    public OrgPermissions() {}

    public OrgPermissions(OrgPermissions permissions, Permission tasklist) {
      this.cluster = permissions;
      this.tasklist = tasklist;
    }

    public Permission getTasklist() {
      return tasklist;
    }

    public OrgPermissions getCluster() {
      return cluster;
    }
  }

  public static class Permission {
    private Boolean read;
    private Boolean create;
    private Boolean update;
    private Boolean delete;

    public Permission() {}

    public Permission(Boolean read, Boolean create, Boolean update, Boolean delete) {
      this.read = read;
      this.create = create;
      this.update = update;
      this.delete = delete;
    }

    public Boolean getRead() {
      return read;
    }

    public Boolean getCreate() {
      return create;
    }

    public Boolean getUpdate() {
      return update;
    }

    public Boolean getDelete() {
      return delete;
    }
  }
}

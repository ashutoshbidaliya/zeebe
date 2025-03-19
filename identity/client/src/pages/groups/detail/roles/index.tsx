/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { FC } from "react";
import { C3EmptyState } from "@camunda/camunda-composite-components";
import { TrashCan } from "@carbon/react/icons";
import useTranslate from "src/utility/localization";
import { useApi } from "src/utility/api/hooks";
import { getRolesByGroupId } from "src/utility/api/groups";
import EntityList from "src/components/entityList";
import { useEntityModal } from "src/components/modal";
import DeleteModal from "src/pages/groups/detail/roles/DeleteModal";
import AssignRolesModal from "src/pages/groups/detail/roles/AssignRolesModal";

type RolesProps = {
  groupId: string;
};

const Roles: FC<RolesProps> = ({ groupId }) => {
  const { t } = useTranslate("groups");

  const {
    data: roles,
    loading,
    success,
    reload,
  } = useApi(getRolesByGroupId, {
    groupId: groupId,
  });

  const isRolesListEmpty = !roles || roles.items?.length === 0;

  const [assignRoles, assignRolesModal] = useEntityModal(
    AssignRolesModal,
    reload,
    {
      assignedRoles: roles?.items || [],
    },
  );
  const openAssignModal = () => assignRoles({ id: groupId });
  const [unassignRole, unassignRoleModal] = useEntityModal(
    DeleteModal,
    reload,
    {
      groupId,
    },
  );

  if (!loading && !success)
    return (
      <C3EmptyState
        heading={t("somethingsWrong")}
        description={t("unableToLoadRoles")}
        button={{ label: t("retry"), onClick: reload }}
      />
    );

  if (success && isRolesListEmpty)
    return (
      <>
        <C3EmptyState
          heading={t("assignRolesToGroup")}
          description={t("groupMemberAccessDisclaimer")}
          button={{
            label: t("assignRole"),
            onClick: openAssignModal,
          }}
          link={{
            label: t("learnMoreAboutGroups"),
            href: `/identity/concepts/access-control/groups`,
          }}
        />
        {assignRolesModal}
      </>
    );

  return (
    <>
      <EntityList
        data={roles?.items}
        headers={[
          { header: t("roleId"), key: "key" },
          { header: t("roleName"), key: "name" },
        ]}
        sortProperty="key"
        loading={loading}
        addEntityLabel={t("assignRole")}
        onAddEntity={openAssignModal}
        searchPlaceholder={t("searchByRoleId")}
        menuItems={[
          {
            label: t("remove"),
            icon: TrashCan,
            isDangerous: true,
            onClick: unassignRole,
          },
        ]}
      />
      {assignRolesModal}
      {unassignRoleModal}
    </>
  );
};

export default Roles;

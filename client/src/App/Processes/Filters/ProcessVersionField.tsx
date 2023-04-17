/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React from 'react';
import {Field, useField, useForm} from 'react-final-form';
import {observer} from 'mobx-react';

import {processesStore} from 'modules/stores/processes';
import {Select} from './styled';
import {IS_COMBOBOX_ENABLED} from 'modules/feature-flags';
import {ComboBox} from 'modules/components/ComboBox';

const ProcessVersionField: React.FC = observer(() => {
  const {versionsByProcess} = processesStore;
  const selectedProcess = useField('process').input.value;

  const versions = versionsByProcess[selectedProcess] ?? [];

  const mappedVersions =
    versions?.map(({version}) => ({
      value: version.toString(),
      label: version.toString(),
    })) ?? [];

  const options = [
    {
      options:
        mappedVersions.length === 1
          ? mappedVersions
          : [{label: 'All', value: 'all'}, ...mappedVersions],
    },
  ];

  const form = useForm();

  return IS_COMBOBOX_ENABLED ? (
    <Field name="version">
      {({input}) => {
        return (
          <ComboBox
            titleText="Version"
            id="processVersion"
            aria-label="Select a Process Version"
            onChange={({selectedItem}) => {
              input.onChange(selectedItem?.id);
              form.change('flowNodeId', undefined);
            }}
            items={
              versions?.map(({version}) => ({
                id: version.toString(),
                label: version.toString(),
              })) ?? []
            }
            value={input.value}
          />
        );
      }}
    </Field>
  ) : (
    <Field name="version">
      {({input}) => {
        return (
          <Select
            label="Version"
            data-testid="filter-process-version"
            onCmInput={(event) => {
              if (event.detail.selectedOptions[0] !== '') {
                input.onChange(event.detail.selectedOptions[0]);

                form.change('flowNodeId', undefined);
              }
            }}
            placeholder="Process Version"
            disabled={versions === undefined || versions.length === 0}
            options={options}
            selectedOptions={
              versions?.length > 0 && input.value
                ? [input.value.toString()]
                : ['all']
            }
          />
        );
      }}
    </Field>
  );
});

export {ProcessVersionField};

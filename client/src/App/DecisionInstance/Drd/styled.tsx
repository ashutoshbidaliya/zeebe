/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import styled, {css} from 'styled-components';
import BasePanelHeader from 'modules/components/Panel/PanelHeader';
import IconButton from 'modules/components/IconButton';

const Container = styled.div`
  ${({theme}) => {
    const colors = theme.colors.modules.diagram.element;

    return css`
      display: grid;
      grid-template-rows: 56px 1fr;
      height: 100%;
      width: 100%;
      background: ${colors.background.default};

      .dmn-drd-container .djs-visual rect {
        stroke: ${colors.border} !important;
        fill: ${colors.background.default} !important;
      }

      .dmn-drd-container .djs-label {
        fill: ${colors.text} !important;
      }

      .dmn-drd-container .djs-connection polyline {
        stroke: ${colors.border} !important;
      }

      marker#information-requirement-end {
        fill: ${colors.border} !important;
      }

      .ope-selectable {
        cursor: pointer;

        &.hover .djs-outline {
          stroke: ${colors.outline};
          stroke-width: 2px;
        }
      }

      .ope-selected {
        .djs-outline {
          stroke: ${colors.outline};
          stroke-width: 2px;
        }

        .djs-visual rect {
          fill: ${colors.background.selected} !important;
        }
      }
    `;
  }}
`;

const PanelHeader = styled(BasePanelHeader)`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  height: auto;
`;

const ButtonContainer = styled.div`
  display: flex;
`;

const Button = styled(IconButton)`
  ${({theme}) => {
    const colors = theme.colors.drdPanel;

    return css`
      svg {
        margin-top: 4px;
      }

      margin: 0 0 0 6px;
      color: ${colors.buttonColor};
    `;
  }}
`;

export {Container, PanelHeader, ButtonContainer, Button};

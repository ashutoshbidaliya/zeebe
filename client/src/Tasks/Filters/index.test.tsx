/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {screen, fireEvent} from '@testing-library/react';
import {render} from 'modules/testing-library';
import {MockThemeProvider} from 'modules/theme/MockProvider';
import {LocationLog} from 'modules/utils/LocationLog';
import {MemoryRouter} from 'react-router-dom';
import {Filters} from './index';

jest.mock('modules/featureFlags');

const createWrapper = (
  initialEntries: React.ComponentProps<
    typeof MemoryRouter
  >['initialEntries'] = ['/'],
) => {
  const Wrapper: React.FC<{
    children?: React.ReactNode;
  }> = ({children}) => (
    <MockThemeProvider>
      <MemoryRouter initialEntries={initialEntries}>
        {children}
        <LocationLog />
      </MemoryRouter>
    </MockThemeProvider>
  );

  return Wrapper;
};

describe('<Filters />', () => {
  it('should filters', () => {
    render(<Filters disabled={false} />, {
      wrapper: createWrapper(),
    });

    fireEvent.click(screen.getByRole('button', {name: 'Filter options'}));

    expect(screen.getByRole('option', {name: 'All open'})).toBeInTheDocument();
    expect(
      screen.getByRole('option', {name: 'Claimed by me'}),
    ).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Unclaimed'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Completed'})).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: 'Sort tasks'}));

    expect(screen.getByText('Creation date')).toBeInTheDocument();
    expect(screen.getByText('Follow-up date')).toBeInTheDocument();
    expect(screen.getByText('Due date')).toBeInTheDocument();
  });

  it('should load values from URL', () => {
    render(<Filters disabled={false} />, {
      wrapper: createWrapper(['/?filter=completed&sortBy=creation']),
    });

    expect(screen.getByText('Completed')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', {name: 'Sort tasks'}));

    expect(screen.getByText('Creation date')).toBeInTheDocument();
  });

  it('should write changes to the URL', () => {
    render(<Filters disabled={false} />, {
      wrapper: createWrapper(),
    });

    fireEvent.click(screen.getByRole('button', {name: 'Filter options'}));
    fireEvent.click(screen.getByRole('option', {name: 'Claimed by me'}));

    fireEvent.click(screen.getByRole('button', {name: 'Sort tasks'}));
    fireEvent.click(screen.getByText('Due date'));

    expect(screen.getByTestId('search')).toHaveTextContent(
      '?filter=claimed-by-me&sortBy=due',
    );
  });

  it('should disable filters', () => {
    render(<Filters disabled={true} />, {
      wrapper: createWrapper(),
    });

    expect(screen.getByRole('button', {name: 'Filter options'})).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Sort tasks'})).toBeDisabled();
  });
});

import React from 'react';
import {shallow, mount} from 'enzyme';
import LogoutButton from './LogoutButton';
import {get} from 'request';

jest.mock('request', () => {
  return {
    get: jest.fn()
  };
});
jest.mock('react-router-dom', () => {
  return {
    Link: ({children, onClick}) => {
      return <a onClick={onClick}>{children}</a>;
    },
    Redirect: ({children, onClick}) => {
      return <a onClick={onClick}>{children}</a>;
    }
  };
});

jest.mock('components', () => ({
  Button: props => {
    return <button {...props}>{props.children}</button>;
  }
}));

it('renders without crashing', () => {
  shallow(<LogoutButton />);
});

it('should logout from server on click', () => {
  const node = mount(<LogoutButton />);

  node.find('button').simulate('click');
  setImmediate(() => {
    expect(get).toHaveBeenCalledWith(expect.stringContaining('logout'));
  });
});

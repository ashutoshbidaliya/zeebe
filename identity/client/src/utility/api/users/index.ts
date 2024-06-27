import { ApiDefinition, apiDelete, apiGet, apiPost, apiPut } from "../request";

export const USERS_ENDPOINT = "/users";

export type User = {
  id: string;
  name: string;
  username: string;
  password: string;
  email: string;
  enabled: boolean;
};

export type UserSearch = {
  items: User[];
};

export const postUserSearch: ApiDefinition<UserSearch> = () =>
  apiPost(`${USERS_ENDPOINT}/search`);

type GetUserParams = {
  id: string;
};

export const getUserDetails: ApiDefinition<User, GetUserParams> = ({ id }) =>
  apiGet(`${USERS_ENDPOINT}/${id}`);

type CreateUserParams = Omit<User, "id" | "enabled">;

export const createUser: ApiDefinition<undefined, CreateUserParams> = (user) =>
  apiPost(USERS_ENDPOINT, { ...user, enabled: true });

type UpdateUserParams = Omit<User, "enabled">;

export const updateUser: ApiDefinition<undefined, UpdateUserParams> = (user) =>
  apiPut(`${USERS_ENDPOINT}/${user.id}`, { ...user, enabled: true });

type DeleteUserParams = GetUserParams;

export const deleteUser: ApiDefinition<undefined, DeleteUserParams> = ({
  id,
}) => apiDelete(`${USERS_ENDPOINT}/${id}`);

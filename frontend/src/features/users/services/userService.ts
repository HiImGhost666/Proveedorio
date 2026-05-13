import api from 'services/api';
import { UserDto, UserRequestDto } from 'src/types';

export const getUsers = async (): Promise<UserDto[]> => {
  const response = await api.get('/admin/usuarios');
  return response.data.data;
};

export const getUserDetails = async (id: number): Promise<UserDto> => {
  const response = await api.get(`/admin/usuarios/${id}`);
  return response.data.data;
};

export const createUser = async (user: UserRequestDto): Promise<UserDto> => {
  const response = await api.post('/admin/usuarios', user);
  return response.data.data;
};

export const updateUser = async (id: number, user: UserRequestDto): Promise<UserDto> => {
  const response = await api.put(`/admin/usuarios/${id}`, user);
  return response.data.data;
};

export const deleteUser = async (id: number): Promise<void> => {
  await api.delete(`/admin/usuarios/${id}`);
};

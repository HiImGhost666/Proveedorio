import api from 'services/api';
import { AuthResponse, LoginCredentials } from 'src/types';



export const login = async (credentials: LoginCredentials): Promise<AuthResponse> => {
  const response = await api.post('/auth/login', credentials);
  return response.data.data;
};

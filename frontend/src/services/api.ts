import axios from 'axios';
import { API_BASE } from '../constants/api';

/**
 * Cliente Axios configurado para la API del backend.
 * Añade JWT en requests y centraliza el manejo de errores de autenticación.
 */
const api = axios.create({
  baseURL: API_BASE,
});


// REQUEST INTERCEPTOR (anade JWT automaticamente)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});


// RESPONSE INTERCEPTOR (manejo de expiracion / acceso denegado y errores globales)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Manejo de fin de sesión
    const requestUrl = error.config?.url ?? '';
    const isLoginRequest = requestUrl.includes('/auth/login');

    if (error.response && [401, 403].includes(error.response.status) && !isLoginRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      // Marcamos que la sesión ha expirado para mostrar un mensaje en la pantalla de login
      try {
        localStorage.setItem('sessionExpired', '1');
      } catch {
        // Si localStorage falla, simplemente redirigimos sin mensaje persistente
      }
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
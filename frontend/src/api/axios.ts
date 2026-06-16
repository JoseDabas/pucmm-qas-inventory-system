import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

// Interceptor para inyectar el token JWT de Keycloak en las cabeceras de cada petición
api.interceptors.request.use((config) => {
  const authority = 'http://localhost:9080/realms/Inventario';
  const clientId = 'inventory-client';
  const oidcStorageKey = `oidc.user:${authority}:${clientId}`;
  
  const oidcStorage = sessionStorage.getItem(oidcStorageKey);

  if (oidcStorage) {
    try {
      const user = JSON.parse(oidcStorage);
      if (user && user.access_token) {
        config.headers.Authorization = `Bearer ${user.access_token}`;
      }
    } catch (e) {
      console.error('Error parsing OIDC user from storage', e);
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default api;

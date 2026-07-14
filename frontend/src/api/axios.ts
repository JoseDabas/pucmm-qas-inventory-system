import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

// Interceptor para inyectar el token JWT de Keycloak en las cabeceras de cada petición
api.interceptors.request.use((config) => {
  const authority = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9080/realms/Inventario';
  const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'inventory-client';
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

  // -------------------------------------------------------------
  // Inyección de W3C Trace Context (Propagación End-to-End)
  // -------------------------------------------------------------
  // Generamos un traceparent estándar (00-<trace_id>-<span_id>-01)
  // para que Grafana Tempo pueda correlacionar la traza desde el clic 
  // en el UI hasta el backend y la base de datos.
  const generateHex = (bytes: number) => {
    const array = new Uint8Array(bytes);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  };
  
  const traceId = generateHex(16); // 32 caracteres hex
  const spanId = generateHex(8);   // 16 caracteres hex
  config.headers.traceparent = `00-${traceId}-${spanId}-01`;

  return config;
}, (error) => {
  return Promise.reject(error);
});

export default api;

import type { AuthProviderProps } from 'react-oidc-context';

export const oidcConfig: AuthProviderProps = {
    // Para entornos Dockerizados, leemos la variable inyectada dinámicamente en window._env_
    // Si no existe (desarrollo local), usamos la variable de entorno de Vite
    authority: (window as any)._env_?.VITE_KEYCLOAK_URL || import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9080/realms/Inventario',
    client_id: (window as any)._env_?.VITE_KEYCLOAK_CLIENT_ID || import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'inventory-client',
    client_secret: (window as any)._env_?.VITE_KEYCLOAK_CLIENT_SECRET || import.meta.env.VITE_KEYCLOAK_CLIENT_SECRET,
    redirect_uri: window.location.origin,
    response_type: 'code',
    scope: 'openid profile email',
    // Opcional: configurar para que renueve el token automáticamente
    automaticSilentRenew: true,
    onSigninCallback: () => {
        // Remover código de la URL después de loguearse exitosamente
        window.history.replaceState({}, document.title, window.location.pathname);
    }
};

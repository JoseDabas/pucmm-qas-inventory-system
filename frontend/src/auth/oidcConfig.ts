import type { AuthProviderProps } from 'react-oidc-context';

export const oidcConfig: AuthProviderProps = {
    authority: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9080/realms/Inventario',
    client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'inventory-client',
    client_secret: import.meta.env.VITE_KEYCLOAK_CLIENT_SECRET,
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

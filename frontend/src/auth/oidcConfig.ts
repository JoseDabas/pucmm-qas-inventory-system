import type { AuthProviderProps } from 'react-oidc-context';

/**
 * Elimina cualquier `state` huérfano que oidc-client-ts haya dejado en el
 * almacenamiento (el `state` se guarda en localStorage y el usuario en
 * sessionStorage) y limpia los parámetros `?state&code` de la URL.
 *
 * Esto evita el bucle de "No matching state found in storage": cuando un
 * callback falla, los parámetros se quedan en la URL y cada recarga reintenta
 * con un `state` que ya fue consumido. Llamar a esto antes de reintentar el
 * login deja el flujo en un estado limpio.
 */
export function clearStaleOidcState(): void {
    for (const storage of [window.localStorage, window.sessionStorage]) {
        Object.keys(storage)
            .filter((key) => key.startsWith('oidc.'))
            .forEach((key) => storage.removeItem(key));
    }
    window.history.replaceState({}, document.title, window.location.pathname);
}

export const oidcConfig: AuthProviderProps = {
    authority: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9080/realms/Inventario',
    client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'inventory-client',
    client_secret: import.meta.env.VITE_KEYCLOAK_CLIENT_SECRET,
    redirect_uri: window.location.origin,
    // URI a la que Keycloak redirige tras cerrar la sesión en el end_session_endpoint.
    post_logout_redirect_uri: window.location.origin,
    response_type: 'code',
    scope: 'openid profile email',
    // Opcional: configurar para que renueve el token automáticamente
    automaticSilentRenew: true,
    onSigninCallback: () => {
        // Remover código de la URL después de loguearse exitosamente
        window.history.replaceState({}, document.title, window.location.pathname);
    }
};

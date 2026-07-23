import { useMemo } from 'react';
import { useAuth } from 'react-oidc-context';

/**
 * Hook que expone los permisos granulares del usuario autenticado.
 *
 * Decodifica el payload del access_token de Keycloak y lee `realm_access.roles`
 * (los permisos del sistema). Es la fuente única de verdad para mostrar u ocultar
 * acciones en la UI. La autorización real la impone el backend por permiso; esto
 * solo mejora la experiencia evitando mostrar lo que el usuario no puede usar.
 */
export const usePermissions = () => {
  const auth = useAuth();
  const token = auth.user?.access_token;

  const permissions = useMemo<string[]>(() => {
    if (!token) return [];
    try {
      const base64Url = token.split('.')[1];
      let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      while (base64.length % 4) {
        base64 += '=';
      }
      const jsonPayload = decodeURIComponent(
        window
          .atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const payload = JSON.parse(jsonPayload);
      return payload.realm_access?.roles ?? [];
    } catch (e) {
      console.error('Error al decodificar el access_token:', e);
      return [];
    }
  }, [token]);

  const hasPermission = (permission: string) => permissions.includes(permission);

  return { permissions, hasPermission };
};

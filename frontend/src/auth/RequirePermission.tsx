import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { usePermissions } from './usePermissions';
import { getRequiredPermission } from '../config/navigation';

interface RequirePermissionProps {
  /** Ruta a proteger; su permiso se resuelve desde la config de navegación. */
  path: string;
  children: ReactNode;
}

/**
 * Guarda de ruta basada en permisos. Renderiza la sección solo si el usuario
 * tiene el permiso requerido por esa ruta; de lo contrario redirige al Dashboard.
 * Complementa el ocultado del menú (Sidebar) para que las secciones ajenas al rol
 * tampoco sean accesibles por URL directa. La autorización real la impone el
 * backend por permiso; esto solo mejora la experiencia en el cliente.
 */
export const RequirePermission = ({ path, children }: RequirePermissionProps) => {
  const { hasPermission } = usePermissions();
  const required = getRequiredPermission(path);

  if (required && !hasPermission(required)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

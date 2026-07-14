import { useLocation } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { LogOut } from 'lucide-react';
import { navItems } from '../config/navigation';

/**
 * Barra superior. Muestra el título de la sección activa (derivado de la ruta
 * usando la config central) y, a la derecha, el usuario y el botón de cerrar sesión.
 */
export const Header: React.FC = () => {
  const location = useLocation();
  const auth = useAuth();

  const current = navItems.find((item) => item.to === location.pathname);
  const title = current?.label ?? 'Inventario';

  const username = auth.user?.profile.preferred_username || auth.user?.profile.name || 'Admin';

  return (
    <header className="h-16 shrink-0 bg-surface border-b border-border flex items-center justify-between px-6">
      <h1 className="text-lg font-semibold text-gray-800">{title}</h1>

      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-500 hidden md:inline">{username}</span>
        <button
          onClick={() => void auth.signoutRedirect()}
          className="btn-secondary flex items-center gap-2 text-sm"
        >
          <LogOut size={16} /> Cerrar Sesión
        </button>
      </div>
    </header>
  );
};

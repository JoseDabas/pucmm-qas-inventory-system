import { NavLink } from 'react-router-dom';
import { navItems } from '../config/navigation';
import { usePermissions } from '../auth/usePermissions';

/**
 * Barra lateral de navegación. Responsabilidad única: mostrar los enlaces
 * a las secciones y resaltar la sección activa. Los ítems vienen de la
 * config central (navItems) y se filtran por el permiso requerido de cada uno.
 */
export const Sidebar: React.FC = () => {
  const { hasPermission } = usePermissions();
  const visibleItems = navItems.filter(
    (item) => !item.requiredPermission || hasPermission(item.requiredPermission)
  );

  return (
    <aside className="w-64 shrink-0 bg-surface border-r border-border flex flex-col">
      <div className="h-16 flex items-center gap-2 px-6 border-b border-border">
        <span className="font-semibold text-gray-800 tracking-tight">Sistema de Inventario</span>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-1">
        {visibleItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              [
                'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-600 hover:bg-surface-hover hover:text-gray-900',
              ].join(' ')
            }
          >
            <Icon size={18} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
};

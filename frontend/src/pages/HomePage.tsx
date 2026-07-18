import { Link } from 'react-router-dom';
import { Package, Tags, BarChart3 } from 'lucide-react';

/**
 * Página de Inicio. Bienvenida sobria con accesos rápidos a las secciones.
 * Contenido estático y honesto (sin datos inventados).
 */
const shortcuts = [
  { to: '/inventario', label: 'Inventario', description: 'Gestiona los productos', icon: Package },
  { to: '/categorias', label: 'Categorías', description: 'Organiza el catálogo', icon: Tags },
  { to: '/reportes', label: 'Reportes', description: 'Consulta indicadores', icon: BarChart3 },
];

export const HomePage: React.FC = () => {
  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="mb-8">
        <h2 className="text-2xl font-semibold text-gray-800">Bienvenido</h2>
        <p className="text-gray-500 mt-1">Sistema de gestión de inventario.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {shortcuts.map(({ to, label, description, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            className="bg-surface border border-border rounded-xl p-5 hover:border-primary-500 transition-colors"
          >
            <Icon className="text-primary-600 mb-3" size={22} />
            <div className="font-medium text-gray-800">{label}</div>
            <p className="text-sm text-gray-500 mt-1">{description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
};

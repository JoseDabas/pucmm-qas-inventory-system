import { LayoutDashboard, Package, History, Tags, BarChart3, Users, Settings } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

/**
 * Fuente única de verdad para la navegación del dashboard.
 * La usan el Sidebar (para renderizar los enlaces) y el Header
 * (para mostrar el título de la sección activa según la ruta).
 */
export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  /**
   * Permiso granular requerido para ver el ítem. Si se omite, el ítem es
   * visible para cualquier usuario autenticado. La autorización real la impone
   * el backend; esto solo evita mostrar secciones que el usuario no puede usar.
   */
  requiredPermission?: string;
}

export const navItems: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/inventario', label: 'Inventario', icon: Package },
  { to: '/historial', label: 'Historial', icon: History },
  { to: '/categorias', label: 'Categorías', icon: Tags },
  { to: '/reportes', label: 'Reportes', icon: BarChart3 },
  { to: '/usuarios', label: 'Usuarios', icon: Users, requiredPermission: 'user:manage' },
  { to: '/configuracion', label: 'Configuración', icon: Settings },
];

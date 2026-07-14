import { Outlet } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar';
import { Header } from '../components/Header';

/**
 * Layout base del dashboard: Sidebar a la izquierda y, a la derecha,
 * el Header sobre el área de contenido. Las páginas se renderizan en <Outlet />.
 */
export const DashboardLayout: React.FC = () => {
  return (
    <div className="flex h-screen bg-background">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Header />
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

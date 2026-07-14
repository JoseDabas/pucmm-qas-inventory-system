import { AuthProvider, useAuth } from 'react-oidc-context';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { oidcConfig } from './auth/oidcConfig';
import { DashboardLayout } from './layouts/DashboardLayout';
import { HomePage } from './pages/HomePage';
import { InventoryPage } from './pages/InventoryPage';
import { PlaceholderPage } from './pages/PlaceholderPage';

const MainContent = () => {
  const auth = useAuth();

  // Lógica OIDC:
  // Mientras se resuelve el estado de la sesión no renderizamos nada.
  // Evita que la pantalla de login parpadee en cada recarga antes de saber si hay sesión.
  if (auth.isLoading) {
    return null;
  }

  // Lógica OIDC:
  // Si ocurrió un error en la redirección o validación del token, lo mostramos.
  if (auth.error) {
    return <div className="text-red-600 text-center p-8">Error de autenticación: {auth.error.message}</div>;
  }

  // Lógica OIDC:
  // Si auth.isAuthenticated es true, el usuario se logueó correctamente en Keycloak.
  // El JWT ya está en SessionStorage y el interceptor de axios lo inyecta en las peticiones.
  // Montamos el router con el layout base del dashboard.
  if (auth.isAuthenticated) {
    return (
      <BrowserRouter>
        <Routes>
          <Route element={<DashboardLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/inventario" element={<InventoryPage />} />
            <Route path="/categorias" element={<PlaceholderPage title="Categorías" />} />
            <Route path="/reportes" element={<PlaceholderPage title="Reportes" />} />
            <Route path="/configuracion" element={<PlaceholderPage title="Configuración" />} />
            {/* Cualquier ruta desconocida cae al inventario (p. ej. /productos/nuevo) */}
            <Route path="*" element={<Navigate to="/inventario" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    );
  }

  // Lógica OIDC:
  // Si no está autenticado, mostramos un botón para iniciar sesión en Keycloak.
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="max-w-md w-full bg-surface border border-border p-8 rounded-2xl shadow-sm text-center">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">Bienvenido</h1>
        <p className="text-gray-500 mb-8">Por favor, inicia sesión para acceder al sistema de inventario empresarial.</p>
        <button
          onClick={() => void auth.signinRedirect()}
          className="btn-primary w-full py-3 text-lg flex justify-center items-center gap-2"
        >
          Iniciar Sesión con SSO
        </button>
      </div>
    </div>
  );
};

function App() {
  return (
    // Envolvemos toda la aplicación en AuthProvider, inyectándole la configuración de Keycloak.
    <AuthProvider {...oidcConfig}>
      <MainContent />
    </AuthProvider>
  );
}

export default App;

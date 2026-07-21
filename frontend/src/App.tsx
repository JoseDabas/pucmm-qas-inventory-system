import { AuthProvider, useAuth } from 'react-oidc-context';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { oidcConfig } from './auth/oidcConfig';
import { DashboardLayout } from './layouts/DashboardLayout';
import { HomePage } from './pages/HomePage';
import { InventoryPage } from './pages/InventoryPage';
import { MovementHistoryPage } from './pages/MovementHistoryPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { PlaceholderPage } from './pages/PlaceholderPage';
import { LoginPage } from './pages/LoginPage';

const MainContent = () => {
  const auth = useAuth();

  // Lógica OIDC:
  // Mientras se resuelve el estado de la sesión no renderizamos nada.
  // Evita que la pantalla de login parpadee en cada recarga antes de saber si hay sesión.
  // Excepción: si el "loading" proviene del envío del formulario (grant password), NO blanqueamos
  // la pantalla; así el LoginPage sigue montado y puede mostrar su spinner y su error en línea.
  if (auth.isLoading && auth.activeNavigator !== 'signinResourceOwnerCredentials') {
    return null;
  }

  // Lógica OIDC:
  // Si ocurrió un error en la redirección o validación del token, lo mostramos a pantalla completa.
  // Excepción: los errores de credenciales del formulario (grant password) NO se muestran aquí;
  // el propio LoginPage los presenta en línea, debajo de los campos, sin perder el formulario.
  if (auth.error && auth.error.source !== 'signinResourceOwnerCredentials') {
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
            <Route path="/historial" element={<MovementHistoryPage />} />
            <Route path="/categorias" element={<CategoriesPage />} />
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
  // Si no está autenticado, mostramos el formulario de login. Las credenciales se validan
  // directamente contra Keycloak mediante el grant "password" (Direct Access Grants), sin
  // redirigir a la pantalla de Keycloak. En caso de éxito, react-oidc-context guarda el JWT
  // en sessionStorage y actualiza isAuthenticated, montando el dashboard automáticamente.
  return (
    <LoginPage
      onLogin={(username, password) =>
        auth.signinResourceOwnerCredentials({ username, password })
      }
    />
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

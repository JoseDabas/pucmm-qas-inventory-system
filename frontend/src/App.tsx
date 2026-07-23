import { AuthProvider, useAuth } from 'react-oidc-context';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { oidcConfig } from './auth/oidcConfig';
import { DashboardLayout } from './layouts/DashboardLayout';
import { DashboardPage } from './pages/DashboardPage';
import { InventoryPage } from './pages/InventoryPage';
import { MovementHistoryPage } from './pages/MovementHistoryPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { UsersPage } from './pages/UsersPage';
import { PlaceholderPage } from './pages/PlaceholderPage';
import { LoginPage } from './pages/LoginPage';
import { RequirePermission } from './auth/RequirePermission';

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
            {/* Dashboard: libre para cualquier usuario autenticado (página de aterrizaje). */}
            <Route path="/dashboard" element={<DashboardPage />} />
            {/* Rutas protegidas: si el rol no tiene el permiso, RequirePermission
                redirige al Dashboard, ocultando la sección también por URL directa. */}
            <Route
              path="/inventario"
              element={<RequirePermission path="/inventario"><InventoryPage /></RequirePermission>}
            />
            <Route
              path="/historial"
              element={<RequirePermission path="/historial"><MovementHistoryPage /></RequirePermission>}
            />
            <Route
              path="/categorias"
              element={<RequirePermission path="/categorias"><CategoriesPage /></RequirePermission>}
            />
            <Route
              path="/usuarios"
              element={<RequirePermission path="/usuarios"><UsersPage /></RequirePermission>}
            />
            <Route
              path="/reportes"
              element={<RequirePermission path="/reportes"><PlaceholderPage title="Reportes" /></RequirePermission>}
            />
            {/* Cualquier ruta desconocida cae al dashboard (p. ej. /productos/nuevo) */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
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
        auth.signinResourceOwnerCredentials({ username, password }).then((user) => {
          
          if (user) {
            window.history.replaceState({}, document.title, '/dashboard');
          }
          return user;
        })
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

import { AuthProvider, useAuth } from 'react-oidc-context';
import { oidcConfig } from './auth/oidcConfig';
import { ProductList } from './components/ProductList';
import { LogOut, Loader2 } from 'lucide-react';

const MainContent = () => {
  const auth = useAuth();

  // Lógica OIDC:
  // Si el contexto de autenticación indica que está cargando el estado del usuario, mostramos un loader
  if (auth.isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="animate-spin text-primary-500" size={48} />
      </div>
    );
  }

  // Lógica OIDC:
  // Si ocurrió un error en la redirección o validación del token, lo mostramos.
  if (auth.error) {
    return <div className="text-red-500 text-center p-8">Error de autenticación: {auth.error.message}</div>;
  }

  // Lógica OIDC:
  // Si auth.isAuthenticated es true, significa que el usuario se logueó correctamente en Keycloak.
  // El JWT ha sido extraído y guardado en SessionStorage de forma automática por oidc-client-ts.
  // El interceptor en src/api/axios.ts lee este token para inyectarlo en las peticiones al backend.
  if (auth.isAuthenticated) {
    return (
      <div>
        <nav className="bg-surface border-b border-border p-4 sticky top-0 z-10 shadow-sm">
          <div className="max-w-7xl mx-auto flex justify-between items-center">
            <div className="font-bold text-xl text-primary-500">InventorySystem</div>
            <div className="flex items-center gap-4">
              <span className="text-gray-400 text-sm hidden md:inline">
                Usuario: {auth.user?.profile.preferred_username || auth.user?.profile.name || 'Admin'}
              </span>
              <button
                onClick={() => void auth.signoutRedirect()}
                className="btn-secondary flex items-center gap-2 text-sm"
              >
                <LogOut size={16} /> Cerrar Sesión
              </button>
            </div>
          </div>
        </nav>
        <ProductList />
      </div>
    );
  }

  // Lógica OIDC:
  // Si no está autenticado, mostramos un botón para iniciar sesión en Keycloak.
  // auth.signinRedirect() redirigirá al usuario a la pantalla de login nativa de Keycloak.
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="max-w-md w-full bg-surface border border-border p-8 rounded-2xl shadow-2xl text-center">
        <h1 className="text-3xl font-bold text-white mb-2">Bienvenido</h1>
        <p className="text-gray-400 mb-8">Por favor, inicia sesión para acceder al sistema de inventario empresarial.</p>
        <button
          onClick={() => void auth.signinRedirect()}
          className="btn-primary w-full py-3 text-lg flex justify-center items-center gap-2 shadow-primary-500/50"
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

import { useState, type FormEvent } from 'react';
import {User, Lock, Eye, EyeOff, AlertTriangle, Loader2 } from 'lucide-react';

interface LoginPageProps {
  // Recibe las credenciales y resuelve la autenticación (grant password contra Keycloak).
  onLogin: (username: string, password: string) => Promise<unknown>;
}

// Pantalla de login (usuario no autenticado).
// Formulario propio de usuario/contraseña que autentica directamente contra Keycloak,
// sin redirigir a su pantalla de inicio de sesión.
export const LoginPage = ({ onLogin }: LoginPageProps) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const user = await onLogin(username.trim(), password);
      // react-oidc-context no lanza excepción al fallar las credenciales: devuelve el User
      // en caso de éxito y null si son inválidas. Por eso comprobamos el valor de retorno.
      if (!user) {
        setError('Usuario o contraseña incorrectos. Inténtalo de nuevo.');
        setPassword(''); // Borrar el campo de contraseña para que el usuario lo reingrese.
        setLoading(false);
      }
      // En caso de éxito, isAuthenticated pasa a true y App monta el dashboard (este componente se desmonta).
    } catch {
      setError('No se pudo conectar con el servidor de autenticación. Inténtalo de nuevo.');
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-background via-background to-primary-50 p-4">
      <div className="max-w-md w-full bg-surface border border-border rounded-2xl p-8">
        {/* Logo de marca + jerarquía tipográfica */}
        <div className="text-center">
          
          <p className="text-s font-bold uppercase  text-primary-600 mb-5">
            Sistema de Inventario
          </p>
        </div>

        {/* Formulario de credenciales */}
        <form onSubmit={handleSubmit} className="space-y-4 text-left">
          <div>
            <label htmlFor="username" className="label-text">
              Usuario o correo
            </label>
            <div className="relative">
              <User size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                id="username"
                type="text"
                autoComplete="username"
                autoFocus
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="usuario o correo"
                className="input-field w-full pl-10"
              />
            </div>
          </div>

          <div>
            <label htmlFor="password" className="label-text">
              Contraseña
            </label>
            <div className="relative">
              <Lock size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="input-field w-full pl-10 pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {error && (
            <div className="flex items-start gap-2 rounded-lg p-3 text-sm text-red-600">
              <AlertTriangle size={18} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full py-3 text-lg flex justify-center items-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading ? (
              <>
                <Loader2 size={20} className="animate-spin" />
                Iniciando sesión...
              </>
            ) : (
              <>
                Iniciar Sesión
              </>
            )}
          </button>
        </form>

      </div>
    </div>
  );
};

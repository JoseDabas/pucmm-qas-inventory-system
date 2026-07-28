import { useEffect, useState } from 'react';
import { Plus, Users } from 'lucide-react';
import { adminApi } from '../api/admin';
import type { SystemRoleOption, User } from '../types/User';
import { UserForm } from '../components/UserForm';

const PAGE_SIZE = 10;

export const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<SystemRoleOption[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [usersRefreshKey, setUsersRefreshKey] = useState(0);
  const [page, setPage] = useState(0);

  const readError = (err: unknown, fallback: string) =>
    (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data?.detail ||
    (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
    fallback;

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        setError(null);
        const data = await adminApi.getUsers();
        if (!cancelled) {
          setUsers(data);
          setPage(0);
        }
      } catch (err: unknown) {
        if (!cancelled) setError(readError(err, 'Error al cargar las cuentas'));
      }
    };
    load();
    return () => { cancelled = true; };
  }, [usersRefreshKey]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const data = await adminApi.getRoles();
        if (!cancelled) setRoles(data);
      } catch (err: unknown) {
        if (!cancelled) setError(readError(err, 'Error al cargar los roles'));
      }
    };
    load();
    return () => { cancelled = true; };
  }, []);

  const refreshUsers = () => setUsersRefreshKey((k) => k + 1);

  const handleFormSave = () => {
    setIsFormOpen(false);
    refreshUsers();
  };

  const handleRoleChange = async (user: User, role: string) => {
    try {
      setError(null);
      await adminApi.changeUserRole(user.id, role);
      refreshUsers();
    } catch (err: unknown) {
      setError(readError(err, 'Error al cambiar el rol'));
    }
  };

  const totalPages = Math.max(1, Math.ceil(users.length / PAGE_SIZE));
  const pagedUsers = users.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
            <Users className="text-primary-600" size={28} />
            Usuarios
          </h1>
          <p className="text-gray-500 mt-1">
            Gestiona las cuentas y sus roles
          </p>
        </div>
        <button
          onClick={() => setIsFormOpen(true)}
          disabled={roles.length === 0}
          data-testid="create-user-button"
          className="btn-primary flex items-center gap-2 disabled:opacity-50"
        >
          <Plus size={20} />
          <span>Crear Cuenta</span>
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/50 text-red-400 p-4 rounded-lg mb-6">
          {error}
        </div>
      )}

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse" data-testid="users-table">
            <thead>
              <tr className="bg-surface-hover border-b border-border text-gray-500 text-sm uppercase tracking-wider">
                <th className="p-4 font-semibold">Usuario</th>
                <th className="p-4 font-semibold">Correo</th>
                <th className="p-4 font-semibold">Permisos</th>
                <th className="p-4 font-semibold">Rol</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {pagedUsers.length === 0 ? (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-gray-400">
                    No hay cuentas registradas.
                  </td>
                </tr>
              ) : (
                pagedUsers.map((user) => (
                  <tr key={user.id} data-testid="user-row">
                    <td className="p-4 text-gray-900 font-medium">
                      {user.firstName || user.lastName
                        ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
                        : user.username}
                      <div className="text-xs text-gray-400">{user.username}</div>
                    </td>
                    <td className="p-4 text-gray-600">{user.email || '—'}</td>
                    <td className="p-4">
                      {user.permissions.length === 0 ? (
                        <span className="text-gray-400">Sin permisos</span>
                      ) : (
                        <div className="flex flex-wrap gap-1.5 max-w-md">
                          {user.permissions.map((permission) => (
                            <span
                              key={permission}
                              className="px-2 py-0.5 text-xs rounded-full bg-primary-50 text-primary-700 border border-primary-100"
                            >
                              {permission}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td className="p-4">
                      <select
                        value={user.role ?? ''}
                        onChange={(e) => handleRoleChange(user, e.target.value)}
                        data-testid="user-role-select"
                        className="input-field py-1.5"
                      >
                        {!user.role && <option value="">Personalizado</option>}
                        {roles.map((role) => (
                          <option key={role.name} value={role.name}>
                            {role.displayName}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex items-center justify-between mt-4">
        <span className="text-sm text-gray-500">
          Página {page + 1} de {totalPages}
        </span>
        <div className="flex gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            data-testid="prev-page-button"
            className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
          >
            Anterior
          </button>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            data-testid="next-page-button"
            className="px-3 py-1.5 border border-border rounded-lg disabled:opacity-50 hover:bg-surface-hover"
          >
            Siguiente
          </button>
        </div>
      </div>

      {isFormOpen && (
        <UserForm roles={roles} onClose={() => setIsFormOpen(false)} onSave={handleFormSave} />
      )}
    </div>
  );
};

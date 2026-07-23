import { useState } from 'react';
import { X } from 'lucide-react';
import { adminApi } from '../api/admin';
import type { CreateUserRequestDTO, SystemRoleOption } from '../types/User';

interface UserFormProps {
  roles: SystemRoleOption[];
  onClose: () => void;
  onSave: () => void;
}

/**
 * Modal para que el administrador cree una cuenta y le asigne un rol
 * (combinación de permisos). El selector de rol se alimenta del catálogo que
 * expone el backend.
 */
export const UserForm: React.FC<UserFormProps> = ({ roles, onClose, onSave }) => {
  const [formData, setFormData] = useState<CreateUserRequestDTO>({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    role: roles[0]?.name ?? '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const selectedRole = roles.find((r) => r.name === formData.role);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await adminApi.createUser({
        username: formData.username.trim(),
        email: formData.email.trim(),
        firstName: formData.firstName?.trim() || undefined,
        lastName: formData.lastName?.trim() || undefined,
        password: formData.password,
        role: formData.role,
      });
      onSave();
    } catch (err: unknown) {
      if ((err as { response?: { status?: number } }).response?.status === 409) {
        setError('Ya existe una cuenta con ese nombre de usuario o correo.');
      } else {
        setError(
          (err as { response?: { data?: { detail?: string; message?: string } } }).response?.data
            ?.detail ||
            (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
            'Error al crear la cuenta'
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-surface border border-border w-full max-w-2xl rounded-2xl shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
        <div className="flex justify-between items-center p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-gray-800">Crear Cuenta</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-800 transition-colors">
            <X size={24} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto custom-scrollbar">
          {error && (
            <div className="mb-4 p-3 bg-red-500/10 border border-red-500/50 text-red-400 rounded-lg text-sm">
              {error}
            </div>
          )}

          <form id="user-form" onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label-text">Nombre de usuario</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                data-testid="user-username"
                required
                className="input-field"
                placeholder="Ej: jperez"
              />
            </div>
            <div>
              <label className="label-text">Correo electrónico</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                data-testid="user-email"
                required
                className="input-field"
                placeholder="Ej: jperez@inventario.local"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label-text">Nombre</label>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  data-testid="user-firstname"
                  className="input-field"
                  placeholder="Juan"
                />
              </div>
              <div>
                <label className="label-text">Apellido</label>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  data-testid="user-lastname"
                  className="input-field"
                  placeholder="Pérez"
                />
              </div>
            </div>
            <div>
              <label className="label-text">Contraseña inicial</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                data-testid="user-password"
                required
                minLength={8}
                className="input-field"
                placeholder="Mínimo 8 caracteres"
              />
            </div>
            <div>
              <label className="label-text">Rol</label>
              <select
                name="role"
                value={formData.role}
                onChange={handleChange}
                data-testid="user-role"
                required
                className="input-field"
              >
                {roles.map((role) => (
                  <option key={role.name} value={role.name}>
                    {role.displayName}
                  </option>
                ))}
              </select>
              {selectedRole && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {selectedRole.permissions.map((permission) => (
                    <span
                      key={permission}
                      className="px-2 py-0.5 text-xs rounded-full bg-primary-50 text-primary-700 border border-primary-100"
                    >
                      {permission}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </form>
        </div>

        <div className="p-6 border-t border-border flex justify-end space-x-3 bg-surface-hover/30">
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancelar
          </button>
          <button
            type="submit"
            form="user-form"
            disabled={loading}
            data-testid="user-submit"
            className="btn-primary flex items-center"
          >
            {loading ? 'Creando...' : 'Crear Cuenta'}
          </button>
        </div>
      </div>
    </div>
  );
};

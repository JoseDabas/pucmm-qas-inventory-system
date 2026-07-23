import api from './axios';
import type { CreateUserRequestDTO, SystemRoleOption, User } from '../types/User';

/**
 * Cliente de la API de administración de cuentas (/api/v1/admin/*).
 * Todas las operaciones requieren el permiso user:manage en el backend.
 */
export const adminApi = {
  getRoles: () => api.get<SystemRoleOption[]>('/api/v1/admin/roles').then((r) => r.data),

  getUsers: () => api.get<User[]>('/api/v1/admin/users').then((r) => r.data),

  createUser: (payload: CreateUserRequestDTO) =>
    api.post<User>('/api/v1/admin/users', payload).then((r) => r.data),

  changeUserRole: (userId: string, role: string) =>
    api.put<User>(`/api/v1/admin/users/${userId}/role`, { role }).then((r) => r.data),
};

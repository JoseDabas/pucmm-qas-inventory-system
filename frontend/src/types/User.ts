export interface SystemRoleOption {
  name: string;
  displayName: string;
  permissions: string[];
}

export interface User {
  id: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  role?: string;
  permissions: string[];
}

export interface CreateUserRequestDTO {
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  password: string;
  role: string;
}

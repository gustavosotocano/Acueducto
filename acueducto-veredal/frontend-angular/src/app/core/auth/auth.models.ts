export type UserRole = 'ADMIN' | 'PRESIDENT' | 'TREASURY';

export interface AuthUser {
  id: string;
  tenantId: string | null;
  fullName: string;
  email: string;
  role: UserRole;
  status: string;
}

export interface LoginResponse {
  accessToken: string;
  user: AuthUser;
}

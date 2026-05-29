import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthUser, UserRole } from './auth.models';
import { TenantStore } from '../tenant/tenant.store';

const TOKEN_KEY = 'acueducto.auth.token';
const USER_KEY = 'acueducto.auth.user';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly tenantStore = inject(TenantStore);

  private readonly tokenSignal = signal<string | null>(this.readToken());
  private readonly userSignal = signal<AuthUser | null>(this.readUser());

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.tokenSignal());
  readonly role = computed(() => this.userSignal()?.role ?? null);

  constructor() {
    const user = this.userSignal();
    if (user?.tenantId) {
      this.tenantStore.setTenantId(user.tenantId);
    }
  }

  setSession(token: string, user: AuthUser) {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    this.tokenSignal.set(token);
    this.userSignal.set(user);
    if (user.tenantId) {
      this.tenantStore.setTenantId(user.tenantId);
    }
  }

  clear() {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    this.tenantStore.clear();
  }

  hasRole(...roles: UserRole[]) {
    const role = this.role();
    return !!role && roles.includes(role);
  }

  canRegisterPayments() {
    return this.hasRole('ADMIN', 'TREASURY');
  }

  canManageTenants() {
    return this.hasRole('ADMIN') && !this.userSignal()?.tenantId;
  }

  canManageUsers() {
    return this.canManageTenants() || this.hasRole('PRESIDENT');
  }

  private readToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  private readUser(): AuthUser | null {
    const raw = sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AppUser {
  id: string;
  tenantId: string | null;
  fullName: string;
  email: string;
  roleCode: string;
  status: string;
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  roleCode: string;
  tenantId?: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  roleCode?: string;
  status?: string;
  password?: string;
}

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  list(tenantId?: string): Observable<AppUser[]> {
    const params = tenantId ? { tenantId } : undefined;
    return this.http.get<AppUser[]>(`${environment.apiBaseUrl}/users`, { params });
  }

  create(request: CreateUserRequest): Observable<AppUser> {
    return this.http.post<AppUser>(`${environment.apiBaseUrl}/users`, request);
  }

  update(id: string, request: UpdateUserRequest): Observable<AppUser> {
    return this.http.put<AppUser>(`${environment.apiBaseUrl}/users/${id}`, request);
  }

  resetPassword(id: string, newPassword: string): Observable<AppUser> {
    return this.http.post<AppUser>(`${environment.apiBaseUrl}/users/${id}/reset-password`, {
      newPassword,
    });
  }
}

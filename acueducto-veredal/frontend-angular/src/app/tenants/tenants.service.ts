import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type Tenant = {
  id: string;
  tenantName: string;
  contactName: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type InitialPresidentRequest = {
  fullName: string;
  email: string;
  password: string;
};

export type CreateTenantRequest = {
  id: string;
  tenantName: string;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  president: InitialPresidentRequest;
};

export type UpdateTenantRequest = {
  tenantName: string;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
};

@Injectable({ providedIn: 'root' })
export class TenantsService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Tenant[]>(`${environment.apiBaseUrl}/tenants`);
  }

  create(payload: CreateTenantRequest) {
    return this.http.post<Tenant>(`${environment.apiBaseUrl}/tenants`, payload);
  }

  update(id: string, payload: UpdateTenantRequest) {
    return this.http.put<Tenant>(`${environment.apiBaseUrl}/tenants/${id}`, payload);
  }

  deactivate(id: string) {
    return this.http.post<Tenant>(`${environment.apiBaseUrl}/tenants/${id}/deactivate`, {});
  }
}


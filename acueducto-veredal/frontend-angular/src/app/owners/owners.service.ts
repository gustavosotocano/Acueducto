import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type Owner = {
  id: string;
  tenantId: string;
  documentType: string;
  documentNumber: string;
  fullName: string;
  firstName: string | null;
  lastName: string | null;
  secondLastName: string | null;
  phone: string | null;
  email: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type CreateOwnerRequest = {
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  secondLastName?: string | null;
  phone?: string | null;
  email?: string | null;
};

export type UpdateOwnerRequest = {
  firstName: string;
  lastName: string;
  secondLastName?: string | null;
  phone?: string | null;
  email?: string | null;
};

@Injectable({ providedIn: 'root' })
export class OwnersService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Owner[]>(`${environment.apiBaseUrl}/owners`);
  }

  create(payload: CreateOwnerRequest) {
    return this.http.post<Owner>(`${environment.apiBaseUrl}/owners`, payload);
  }

  update(id: string, payload: UpdateOwnerRequest) {
    return this.http.put<Owner>(`${environment.apiBaseUrl}/owners/${id}`, payload);
  }

  deactivate(id: string) {
    return this.http.post<Owner>(`${environment.apiBaseUrl}/owners/${id}/deactivate`, {});
  }
}


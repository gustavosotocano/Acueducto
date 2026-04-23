import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type Property = {
  id: string;
  tenantId: string;
  propertyCode: string;
  propertyName: string;
  addressLine: string | null;
  ownerId: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type CreatePropertyRequest = {
  propertyCode: string;
  propertyName: string;
  addressLine: string;
  ownerId: string;
  ownershipStartDate: string; // YYYY-MM-DD
};

export type UpdatePropertyRequest = {
  propertyName: string;
  addressLine: string;
};

@Injectable({ providedIn: 'root' })
export class PropertiesService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Property[]>(`${environment.apiBaseUrl}/properties`);
  }

  create(payload: CreatePropertyRequest) {
    return this.http.post<Property>(`${environment.apiBaseUrl}/properties`, payload);
  }

  update(id: string, payload: UpdatePropertyRequest) {
    return this.http.put<Property>(`${environment.apiBaseUrl}/properties/${id}`, payload);
  }

  deactivate(id: string) {
    return this.http.post<Property>(`${environment.apiBaseUrl}/properties/${id}/deactivate`, {});
  }
}


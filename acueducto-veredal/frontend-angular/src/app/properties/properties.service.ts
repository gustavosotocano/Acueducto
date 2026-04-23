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

@Injectable({ providedIn: 'root' })
export class PropertiesService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Property[]>(`${environment.apiBaseUrl}/properties`);
  }
}


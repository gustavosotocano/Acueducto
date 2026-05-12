import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type ConnectionTariff = {
  id: string;
  tenantId: string;
  connectionType: string;
  amount: number;
  validFrom: string;
  validTo: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateConnectionTariffRequest = {
  connectionType: string;
  amount: number;
  validFrom: string;
  validTo?: string | null;
};

export type UpdateConnectionTariffRequest = {
  amount: number;
  validFrom: string;
  validTo?: string | null;
};

@Injectable({ providedIn: 'root' })
export class ConnectionTariffsService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<ConnectionTariff[]>(`${environment.apiBaseUrl}/connection-tariffs`);
  }

  create(payload: CreateConnectionTariffRequest) {
    return this.http.post<ConnectionTariff>(`${environment.apiBaseUrl}/connection-tariffs`, payload);
  }

  update(id: string, payload: UpdateConnectionTariffRequest) {
    return this.http.put<ConnectionTariff>(`${environment.apiBaseUrl}/connection-tariffs/${id}`, payload);
  }
}

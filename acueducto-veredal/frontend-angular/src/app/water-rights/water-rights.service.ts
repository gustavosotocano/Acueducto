import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type WaterRight = {
  id: string;
  tenantId: string;
  rightCode: string;
  ownerId: string;
  propertyId: string;
  waterMeterId: string | null;
  serviceStatus: string;
  connectionType: ConnectionType;
  baseTariff: number;
  ownershipStartDate: string;
  ownershipEndDate: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ConnectionType =
  | 'RESIDENTIAL'
  | 'COMMERCIAL'
  | 'AGRICULTURAL'
  | 'SHARED'
  | 'METERED'
  | 'UNMETERED';

export type CreateWaterRightRequest = {
  rightCode: string;
  ownerId: string;
  propertyId: string;
  waterMeterId?: string | null;
  serviceStatus: string;
  connectionType: ConnectionType;
  baseTariff: number;
  ownershipStartDate: string; // YYYY-MM-DD
};

export type UpdateWaterRightRequest = {
  propertyId: string;
  waterMeterId?: string | null;
  serviceStatus: string;
  connectionType: ConnectionType;
  baseTariff: number;
};

export type DeactivateWaterRightRequest = {
  ownershipEndDate: string; // YYYY-MM-DD
};

@Injectable({ providedIn: 'root' })
export class WaterRightsService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<WaterRight[]>(`${environment.apiBaseUrl}/water-rights`);
  }

  create(payload: CreateWaterRightRequest) {
    return this.http.post<WaterRight>(`${environment.apiBaseUrl}/water-rights`, payload);
  }

  update(id: string, payload: UpdateWaterRightRequest) {
    return this.http.put<WaterRight>(`${environment.apiBaseUrl}/water-rights/${id}`, payload);
  }

  deactivate(id: string, payload: DeactivateWaterRightRequest) {
    return this.http.post<WaterRight>(`${environment.apiBaseUrl}/water-rights/${id}/deactivate`, payload);
  }
}


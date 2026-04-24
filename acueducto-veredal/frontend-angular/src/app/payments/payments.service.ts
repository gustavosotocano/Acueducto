import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type Payment = {
  id: string;
  tenantId: string;
  invoiceId: string;
  waterRightId: string;
  ownerId: string;
  paymentDate: string;
  amount: number;
  method: string;
  reference: string | null;
  notes: string | null;
  createdAt: string;
};

export type CreatePaymentRequest = {
  invoiceId: string;
  paymentDate: string; // yyyy-mm-dd
  amount: number;
  method: string;
  reference?: string | null;
  notes?: string | null;
};

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  private readonly http = inject(HttpClient);

  list(filters?: { invoiceId?: string; ownerId?: string; waterRightId?: string }) {
    let params = new HttpParams();
    if (filters?.invoiceId) params = params.set('invoiceId', filters.invoiceId);
    if (filters?.ownerId) params = params.set('ownerId', filters.ownerId);
    if (filters?.waterRightId) params = params.set('waterRightId', filters.waterRightId);
    return this.http.get<Payment[]>(`${environment.apiBaseUrl}/payments`, { params });
  }

  create(payload: CreatePaymentRequest) {
    return this.http.post<Payment>(`${environment.apiBaseUrl}/payments`, payload);
  }
}


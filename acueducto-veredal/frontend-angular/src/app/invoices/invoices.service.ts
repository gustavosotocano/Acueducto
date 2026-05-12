import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type Invoice = {
  id: string;
  tenantId: string;
  waterRightId: string;
  ownerId: string;
  invoiceNumber: string;
  status: string;
  periodStart: string;
  periodEnd: string;
  issueDate: string;
  dueDate: string;
  totalAmount: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateInvoiceRequest = {
  waterRightId: string;
  invoiceNumber: string;
  periodStart: string; // yyyy-mm-dd
  periodEnd: string; // yyyy-mm-dd
  issueDate: string; // yyyy-mm-dd
  dueDate: string; // yyyy-mm-dd
  totalAmount: number;
  notes?: string | null;
};

/** RF-060: POST /invoices/batch/monthly */
export type BatchMonthlyInvoiceRequest = {
  billingYear: number;
  billingMonth: number;
  issueDate?: string | null;
  dueDate?: string | null;
};

export type BatchMonthlySkip = {
  waterRightId: string;
  reason: string;
};

export type BatchMonthlyInvoiceResponse = {
  billingYear: number;
  billingMonth: number;
  periodStart: string;
  periodEnd: string;
  issueDate: string;
  dueDate: string;
  createdCount: number;
  skippedCount: number;
  createdInvoiceIds: string[];
  skipped: BatchMonthlySkip[];
};

@Injectable({ providedIn: 'root' })
export class InvoicesService {
  private readonly http = inject(HttpClient);

  list(filters?: { ownerId?: string; waterRightId?: string; status?: string }) {
    let params = new HttpParams();
    if (filters?.ownerId) params = params.set('ownerId', filters.ownerId);
    if (filters?.waterRightId) params = params.set('waterRightId', filters.waterRightId);
    if (filters?.status) params = params.set('status', filters.status);

    return this.http.get<Invoice[]>(`${environment.apiBaseUrl}/invoices`, { params });
  }

  create(payload: CreateInvoiceRequest) {
    return this.http.post<Invoice>(`${environment.apiBaseUrl}/invoices`, payload);
  }

  runMonthlyBatch(payload: BatchMonthlyInvoiceRequest) {
    return this.http.post<BatchMonthlyInvoiceResponse>(
      `${environment.apiBaseUrl}/invoices/batch/monthly`,
      payload,
    );
  }
}


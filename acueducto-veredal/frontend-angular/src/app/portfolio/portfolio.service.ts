import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export type WaterRightBalance = {
  waterRightId: string;
  ownerId: string;
  billedAmount: number;
  paidAmount: number;
  outstandingAmount: number;
};

export type OwnerBalance = {
  ownerId: string;
  billedAmount: number;
  paidAmount: number;
  outstandingAmount: number;
};

export type OverdueBucket = {
  bucket: '0-30' | '31-60' | '61-90' | '>90';
  invoiceCount: number;
  outstandingAmount: number;
};

export type OverdueBucketWithInterest = OverdueBucket & {
  interestAmount: number;
  totalWithInterest: number;
};

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly http = inject(HttpClient);

  outstandingByWaterRight() {
    return this.http.get<WaterRightBalance[]>(`${environment.apiBaseUrl}/portfolio/water-rights`);
  }

  outstandingByOwner() {
    return this.http.get<OwnerBalance[]>(`${environment.apiBaseUrl}/portfolio/owners`);
  }

  overdueBuckets() {
    return this.http.get<OverdueBucket[]>(`${environment.apiBaseUrl}/portfolio/overdue`);
  }

  overdueBucketsWithInterest() {
    return this.http.get<OverdueBucketWithInterest[]>(`${environment.apiBaseUrl}/portfolio/overdue-with-interest`);
  }
}


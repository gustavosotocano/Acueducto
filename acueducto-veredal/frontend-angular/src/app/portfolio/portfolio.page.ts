import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { catchError, combineLatest, map, of, shareReplay, startWith } from 'rxjs';
import { PortfolioService } from './portfolio.service';

@Component({
  selector: 'app-portfolio-page',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    MatCardModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 style="margin: 0">Portfolio</h2>
        <p style="margin: 4px 0 0; color: var(--mat-sys-on-surface-variant)">
          Saldo pendiente y cartera vencida por rangos de días.
        </p>
      </div>
    </div>

    @if (vm$ | async; as vm) {
      <div class="grid">
        <mat-card class="card">
          <mat-card-header>
            <mat-card-title>Cartera vencida (con intereses)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="vm.overdue" class="mat-elevation-z0" style="width: 100%">
              <ng-container matColumnDef="bucket">
                <th mat-header-cell *matHeaderCellDef>Rango</th>
                <td mat-cell *matCellDef="let r">{{ r.bucket }}</td>
              </ng-container>

              <ng-container matColumnDef="invoiceCount">
                <th mat-header-cell *matHeaderCellDef># Facturas</th>
                <td mat-cell *matCellDef="let r">{{ r.invoiceCount }}</td>
              </ng-container>

              <ng-container matColumnDef="outstandingAmount">
                <th mat-header-cell *matHeaderCellDef>Saldo</th>
                <td mat-cell *matCellDef="let r">{{ r.outstandingAmount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
              </ng-container>

              <ng-container matColumnDef="interestAmount">
                <th mat-header-cell *matHeaderCellDef>Interés</th>
                <td mat-cell *matCellDef="let r">{{ r.interestAmount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
              </ng-container>

              <ng-container matColumnDef="totalWithInterest">
                <th mat-header-cell *matHeaderCellDef>Total</th>
                <td mat-cell *matCellDef="let r">
                  {{ r.totalWithInterest | currency : 'COP' : 'symbol' : '1.0-0' }}
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="overdueColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: overdueColumns"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <mat-card class="card">
          <mat-card-header>
            <mat-card-title>Saldo por owner</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="vm.byOwner" class="mat-elevation-z0" style="width: 100%">
              <ng-container matColumnDef="ownerId">
                <th mat-header-cell *matHeaderCellDef>Owner</th>
                <td mat-cell *matCellDef="let r" class="mono">{{ r.ownerId }}</td>
              </ng-container>

              <ng-container matColumnDef="outstandingAmount">
                <th mat-header-cell *matHeaderCellDef>Saldo pendiente</th>
                <td mat-cell *matCellDef="let r">{{ r.outstandingAmount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="ownerColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: ownerColumns"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </div>

      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Saldo por water right</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="vm.byWaterRight" class="mat-elevation-z0" style="width: 100%">
            <ng-container matColumnDef="waterRightId">
              <th mat-header-cell *matHeaderCellDef>Water right</th>
              <td mat-cell *matCellDef="let r" class="mono">{{ r.waterRightId }}</td>
            </ng-container>

            <ng-container matColumnDef="ownerId">
              <th mat-header-cell *matHeaderCellDef>Owner</th>
              <td mat-cell *matCellDef="let r" class="mono">{{ r.ownerId }}</td>
            </ng-container>

            <ng-container matColumnDef="outstandingAmount">
              <th mat-header-cell *matHeaderCellDef>Saldo pendiente</th>
              <td mat-cell *matCellDef="let r">
                {{ r.outstandingAmount | currency : 'COP' : 'symbol' : '1.0-0' }}
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="waterRightColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: waterRightColumns"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    } @else {
      <div style="display: grid; place-items: center; height: 240px">
        <mat-spinner diameter="28" />
      </div>
    }
  `,
  styleUrl: './portfolio.page.scss',
})
export class PortfolioPage {
  private readonly portfolioService = inject(PortfolioService);

  protected readonly overdueColumns = [
    'bucket',
    'invoiceCount',
    'outstandingAmount',
    'interestAmount',
    'totalWithInterest',
  ];
  protected readonly ownerColumns = ['ownerId', 'outstandingAmount'];
  protected readonly waterRightColumns = ['waterRightId', 'ownerId', 'outstandingAmount'];

  private readonly overdue$ = this.portfolioService.overdueBucketsWithInterest().pipe(catchError(() => of([])));
  private readonly byOwner$ = this.portfolioService.outstandingByOwner().pipe(catchError(() => of([])));
  private readonly byWaterRight$ = this.portfolioService.outstandingByWaterRight().pipe(catchError(() => of([])));

  protected readonly vm$ = combineLatest([this.overdue$, this.byOwner$, this.byWaterRight$]).pipe(
    map(([overdue, byOwner, byWaterRight]) => ({
      overdue,
      byOwner,
      byWaterRight,
    })),
    startWith(null),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
}


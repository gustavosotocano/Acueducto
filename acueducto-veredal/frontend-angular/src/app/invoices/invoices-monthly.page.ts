import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, of, tap } from 'rxjs';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { toIsoDate } from './invoices-date.util';
import {
  BatchMonthlyInvoiceRequest,
  BatchMonthlyInvoiceResponse,
  InvoicesService,
} from './invoices.service';

@Component({
  selector: 'app-invoices-monthly-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header
      title="Factura mensual"
      subtitle="Generación por lote (RF-060) para todos los derechos de agua activos del tenant."
    />

    <mat-card class="card">
      <mat-card-header>
        <mat-card-title>Facturación mensual por lote</mat-card-title>
        <mat-card-subtitle>
          Las facturas omitidas no duplican un periodo ya facturado para el mismo derecho.
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="batchForm" class="form" (ngSubmit)="submitBatch()">
          <mat-form-field appearance="outline">
            <mat-label>Año de cobro</mat-label>
            <input matInput type="number" formControlName="billingYear" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Mes de cobro</mat-label>
            <mat-select formControlName="billingMonth">
              @for (m of months; track m) {
                <mat-option [value]="m">{{ monthLabel(m) }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <p class="hint">
            Fechas opcionales: si las deja vacías, el backend usa fin de mes como emisión y +15 días para
            vencimiento.
          </p>

          <mat-form-field appearance="outline">
            <mat-label>Fecha emisión (opcional)</mat-label>
            <input matInput [matDatepicker]="bIssue" formControlName="issueDate" />
            <mat-datepicker-toggle matIconSuffix [for]="bIssue" />
            <mat-datepicker #bIssue />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Vence (opcional)</mat-label>
            <input matInput [matDatepicker]="bDue" formControlName="dueDate" />
            <mat-datepicker-toggle matIconSuffix [for]="bDue" />
            <mat-datepicker #bDue />
          </mat-form-field>

          <div class="actions">
            <button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="batchForm.invalid || (batchSaving$ | async)"
            >
              @if (batchSaving$ | async) {
                <mat-spinner diameter="18" />
              } @else {
                <mat-icon>calendar_month</mat-icon>
              }
              <span>Ejecutar lote mensual</span>
            </button>
          </div>
        </form>

        @if (lastBatch) {
          <div class="result-skips">
            <mat-divider />
            <p class="result-summary">
              <strong>Resultado:</strong> creadas {{ lastBatch.createdCount }}, omitidas
              {{ lastBatch.skippedCount }}. Periodo {{ lastBatch.periodStart }} → {{ lastBatch.periodEnd }},
              emisión {{ lastBatch.issueDate }}, vence {{ lastBatch.dueDate }}.
            </p>
            @if (lastBatch.skipped.length > 0) {
              <table mat-table [dataSource]="lastBatch.skipped" class="mat-elevation-z0 skips-table">
                <ng-container matColumnDef="waterRightId">
                  <th mat-header-cell *matHeaderCellDef>Water right</th>
                  <td mat-cell *matCellDef="let s">{{ s.waterRightId }}</td>
                </ng-container>
                <ng-container matColumnDef="reason">
                  <th mat-header-cell *matHeaderCellDef>Motivo</th>
                  <td mat-cell *matCellDef="let s">{{ s.reason }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="skipColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: skipColumns"></tr>
              </table>
            }
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './invoices-monthly.page.scss',
})
export class InvoicesMonthlyPage {
  private readonly invoicesService = inject(InvoicesService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly months = Array.from({ length: 12 }, (_, i) => i + 1);
  protected readonly skipColumns = ['waterRightId', 'reason'];
  protected lastBatch: BatchMonthlyInvoiceResponse | null = null;

  private readonly batchSavingSubject = new BehaviorSubject<boolean>(false);
  protected readonly batchSaving$ = this.batchSavingSubject.asObservable();

  protected readonly batchForm = this.fb.group({
    billingYear: [
      new Date().getFullYear(),
      [Validators.required, Validators.min(2000), Validators.max(3000)],
    ],
    billingMonth: [new Date().getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12)]],
    issueDate: [null as Date | null],
    dueDate: [null as Date | null],
  });

  submitBatch() {
    if (this.batchForm.invalid || this.batchSavingSubject.value) return;
    const v = this.batchForm.getRawValue();
    const payload: BatchMonthlyInvoiceRequest = {
      billingYear: Number(v.billingYear),
      billingMonth: Number(v.billingMonth),
    };
    if (v.issueDate) {
      payload.issueDate = toIsoDate(v.issueDate);
    }
    if (v.dueDate) {
      payload.dueDate = toIsoDate(v.dueDate);
    }

    this.lastBatch = null;
    this.batchSavingSubject.next(true);
    this.invoicesService
      .runMonthlyBatch(payload)
      .pipe(
        tap((res) => {
          this.lastBatch = res;
          this.snackBar.open(`Lote: ${res.createdCount} creadas, ${res.skippedCount} omitidas`, 'OK', {
            duration: 5000,
          });
        }),
        catchError((err) => {
          const msg = err?.error?.message || 'Error en lote mensual';
          this.snackBar.open(msg, 'OK', { duration: 6000 });
          return of(null);
        }),
        tap(() => this.batchSavingSubject.next(false)),
      )
      .subscribe();
  }

  protected monthLabel(m: number): string {
    const names = [
      'Enero',
      'Febrero',
      'Marzo',
      'Abril',
      'Mayo',
      'Junio',
      'Julio',
      'Agosto',
      'Septiembre',
      'Octubre',
      'Noviembre',
      'Diciembre',
    ];
    return names[m - 1] ?? String(m);
  }
}

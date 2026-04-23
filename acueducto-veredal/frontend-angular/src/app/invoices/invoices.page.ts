import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, map, of, shareReplay, startWith, switchMap, tap } from 'rxjs';
import { CreateInvoiceRequest, InvoicesService } from './invoices.service';

@Component({
  selector: 'app-invoices-page',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    DatePipe,
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
  ],
  template: `
    <div class="header">
      <div>
        <h2 style="margin: 0">Invoices</h2>
        <p style="margin: 4px 0 0; color: var(--mat-sys-on-surface-variant)">
          Crear factura manual y consultar listado.
        </p>
      </div>
    </div>

    <div class="grid">
      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Nueva factura</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" class="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>Water right ID</mat-label>
              <input matInput formControlName="waterRightId" placeholder="UUID" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Número</mat-label>
              <input matInput formControlName="invoiceNumber" placeholder="FAC-0001" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Periodo inicio</mat-label>
              <input matInput [matDatepicker]="ps" formControlName="periodStart" />
              <mat-datepicker-toggle matIconSuffix [for]="ps" />
              <mat-datepicker #ps />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Periodo fin</mat-label>
              <input matInput [matDatepicker]="pe" formControlName="periodEnd" />
              <mat-datepicker-toggle matIconSuffix [for]="pe" />
              <mat-datepicker #pe />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha emisión</mat-label>
              <input matInput [matDatepicker]="idp" formControlName="issueDate" />
              <mat-datepicker-toggle matIconSuffix [for]="idp" />
              <mat-datepicker #idp />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Vence</mat-label>
              <input matInput [matDatepicker]="ddp" formControlName="dueDate" />
              <mat-datepicker-toggle matIconSuffix [for]="ddp" />
              <mat-datepicker #ddp />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Total</mat-label>
              <input matInput type="number" formControlName="totalAmount" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="notes">
              <mat-label>Notas</mat-label>
              <textarea matInput rows="3" formControlName="notes"></textarea>
            </mat-form-field>

            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || (saving$ | async)">
                @if (saving$ | async) {
                  <mat-spinner diameter="18" />
                } @else {
                  <mat-icon>add</mat-icon>
                }
                <span>Crear</span>
              </button>
              <button mat-button type="button" (click)="reset()">Limpiar</button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Listado</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (invoices$ | async; as invoices) {
            <table mat-table [dataSource]="invoices" class="mat-elevation-z0" style="width: 100%">
              <ng-container matColumnDef="invoiceNumber">
                <th mat-header-cell *matHeaderCellDef>Número</th>
                <td mat-cell *matCellDef="let r">{{ r.invoiceNumber }}</td>
              </ng-container>

              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Estado</th>
                <td mat-cell *matCellDef="let r">{{ r.status }}</td>
              </ng-container>

              <ng-container matColumnDef="issueDate">
                <th mat-header-cell *matHeaderCellDef>Emisión</th>
                <td mat-cell *matCellDef="let r">{{ r.issueDate | date : 'yyyy-MM-dd' }}</td>
              </ng-container>

              <ng-container matColumnDef="dueDate">
                <th mat-header-cell *matHeaderCellDef>Vence</th>
                <td mat-cell *matCellDef="let r">{{ r.dueDate | date : 'yyyy-MM-dd' }}</td>
              </ng-container>

              <ng-container matColumnDef="totalAmount">
                <th mat-header-cell *matHeaderCellDef>Total</th>
                <td mat-cell *matCellDef="let r">{{ r.totalAmount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns"></tr>
            </table>
          } @else {
            <div style="display: grid; place-items: center; height: 200px">
              <mat-spinner diameter="28" />
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styleUrl: './invoices.page.scss',
})
export class InvoicesPage {
  private readonly invoicesService = inject(InvoicesService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly savingSubject = new BehaviorSubject<boolean>(false);
  protected readonly saving$ = this.savingSubject.asObservable();

  protected readonly columns = ['invoiceNumber', 'status', 'issueDate', 'dueDate', 'totalAmount'];

  protected readonly invoices$ = this.refresh$.pipe(
    switchMap(() => this.invoicesService.list()),
    catchError(() => of([])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly form = this.fb.nonNullable.group({
    waterRightId: ['', [Validators.required]],
    invoiceNumber: ['', [Validators.required]],
    periodStart: [new Date(), [Validators.required]],
    periodEnd: [new Date(), [Validators.required]],
    issueDate: [new Date(), [Validators.required]],
    dueDate: [new Date(), [Validators.required]],
    totalAmount: [25000, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  submit() {
    if (this.form.invalid || this.savingSubject.value) return;

    const v = this.form.getRawValue();
    const payload: CreateInvoiceRequest = {
      waterRightId: v.waterRightId,
      invoiceNumber: v.invoiceNumber,
      periodStart: toIsoDate(v.periodStart),
      periodEnd: toIsoDate(v.periodEnd),
      issueDate: toIsoDate(v.issueDate),
      dueDate: toIsoDate(v.dueDate),
      totalAmount: Number(v.totalAmount),
      notes: v.notes || null,
    };

    this.savingSubject.next(true);
    this.invoicesService
      .create(payload)
      .pipe(
        tap(() => this.snackBar.open('Factura creada', 'OK', { duration: 2500 })),
        tap(() => this.refresh$.next()),
        catchError((err) => {
          const msg = err?.error?.message || 'Error creando factura';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of(null);
        }),
        tap(() => this.savingSubject.next(false)),
      )
      .subscribe();
  }

  reset() {
    this.form.reset({
      waterRightId: '',
      invoiceNumber: '',
      periodStart: new Date(),
      periodEnd: new Date(),
      issueDate: new Date(),
      dueDate: new Date(),
      totalAmount: 25000,
      notes: '',
    });
  }
}

function toIsoDate(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}


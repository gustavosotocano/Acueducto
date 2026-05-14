import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, combineLatest, map, of, shareReplay, switchMap, take, tap } from 'rxjs';
import { Invoice, InvoicesService } from '../invoices/invoices.service';
import { CreatePaymentRequest, Payment, PaymentsService } from './payments.service';

@Component({
  selector: 'app-payments-page',
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
    MatSelectModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 style="margin: 0">Payments</h2>
        <p style="margin: 4px 0 0; color: var(--mat-sys-on-surface-variant)">
          RF-059: primero cree la factura y luego registre el pago asociado.
        </p>
      </div>
    </div>

    <div class="grid">
      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Registrar pago</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" class="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full">
              <mat-label>Factura</mat-label>
              <mat-select formControlName="invoiceId" (selectionChange)="onInvoiceSelected($event.value)">
                @if (payableInvoices$ | async; as invoices) {
                  @if (invoices.length === 0) {
                    <mat-option disabled>No hay facturas disponibles. Cree una en Invoices.</mat-option>
                  } @else {
                    @for (invoice of invoices; track invoice.id) {
                      <mat-option [value]="invoice.id">
                        {{ invoice.invoiceNumber }} · {{ invoice.status }} ·
                        {{ invoice.totalAmount | currency : 'COP' : 'symbol' : '1.0-0' }}
                      </mat-option>
                    }
                  }
                } @else {
                  <mat-option disabled>Cargando facturas…</mat-option>
                }
              </mat-select>
              <mat-hint>El recibo de pago queda ligado a la factura seleccionada.</mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Fecha</mat-label>
              <input matInput [matDatepicker]="pd" formControlName="paymentDate" />
              <mat-datepicker-toggle matIconSuffix [for]="pd" />
              <mat-datepicker #pd />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Monto</mat-label>
              <input matInput type="number" formControlName="amount" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Método</mat-label>
              <mat-select formControlName="method">
                <mat-option value="CASH">Cash</mat-option>
                <mat-option value="TRANSFER">Transfer</mat-option>
                <mat-option value="CARD">Card</mat-option>
                <mat-option value="OTHER">Other</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Referencia</mat-label>
              <input matInput formControlName="reference" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full">
              <mat-label>Notas</mat-label>
              <textarea matInput rows="3" formControlName="notes"></textarea>
            </mat-form-field>

            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || (saving$ | async)">
                @if (saving$ | async) {
                  <mat-spinner diameter="18" />
                } @else {
                  <mat-icon>payments</mat-icon>
                }
                <span>Registrar</span>
              </button>
              <button mat-button type="button" (click)="reset()">Limpiar</button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Historial</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (payments$ | async; as payments) {
            <table mat-table [dataSource]="payments" class="mat-elevation-z0" style="width: 100%">
              <ng-container matColumnDef="paymentDate">
                <th mat-header-cell *matHeaderCellDef>Fecha</th>
                <td mat-cell *matCellDef="let r">{{ r.paymentDate | date : 'yyyy-MM-dd' }}</td>
              </ng-container>

              <ng-container matColumnDef="amount">
                <th mat-header-cell *matHeaderCellDef>Monto</th>
                <td mat-cell *matCellDef="let r">{{ r.amount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
              </ng-container>

              <ng-container matColumnDef="method">
                <th mat-header-cell *matHeaderCellDef>Método</th>
                <td mat-cell *matCellDef="let r">{{ r.method }}</td>
              </ng-container>

              <ng-container matColumnDef="invoiceNumber">
                <th mat-header-cell *matHeaderCellDef>Factura</th>
                <td mat-cell *matCellDef="let r">{{ r.invoiceNumber }}</td>
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
  styleUrl: './payments.page.scss',
})
export class PaymentsPage {
  private readonly paymentsService = inject(PaymentsService);
  private readonly invoicesService = inject(InvoicesService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly savingSubject = new BehaviorSubject<boolean>(false);
  protected readonly saving$ = this.savingSubject.asObservable();

  protected readonly columns = ['paymentDate', 'amount', 'method', 'invoiceNumber'];

  protected readonly allInvoices$ = this.invoicesService.list().pipe(
    catchError(() => of([] as Invoice[])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly payableInvoices$ = this.allInvoices$.pipe(
    map((invoices) => invoices.filter((invoice) => invoice.status !== 'CANCELLED')),
  );

  protected readonly payments$ = this.refresh$.pipe(
    switchMap(() =>
      combineLatest([this.paymentsService.list(), this.allInvoices$]).pipe(
        map(([payments, invoices]) => attachInvoiceNumbers(payments, invoices)),
      ),
    ),
    catchError(() => of([] as PaymentRow[])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly form = this.fb.nonNullable.group({
    invoiceId: ['', [Validators.required]],
    paymentDate: [new Date(), [Validators.required]],
    amount: [25000, [Validators.required, Validators.min(0.01)]],
    method: ['CASH', [Validators.required]],
    reference: [''],
    notes: [''],
  });

  onInvoiceSelected(invoiceId: string) {
    this.payableInvoices$
      .pipe(
        take(1),
        map((invoices) => invoices.find((invoice) => invoice.id === invoiceId)),
        tap((invoice) => {
          if (invoice) {
            this.form.patchValue({ amount: invoice.totalAmount });
          }
        }),
      )
      .subscribe();
  }

  submit() {
    if (this.form.invalid || this.savingSubject.value) return;

    const v = this.form.getRawValue();
    const payload: CreatePaymentRequest = {
      invoiceId: v.invoiceId,
      paymentDate: toIsoDate(v.paymentDate),
      amount: Number(v.amount),
      method: v.method,
      reference: v.reference || null,
      notes: v.notes || null,
    };

    this.savingSubject.next(true);
    this.paymentsService
      .create(payload)
      .pipe(
        tap(() => this.snackBar.open('Pago registrado', 'OK', { duration: 2500 })),
        tap(() => this.refresh$.next()),
        catchError((err) => {
          const msg = err?.error?.message || 'Error registrando pago';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of(null);
        }),
        tap(() => this.savingSubject.next(false)),
      )
      .subscribe();
  }

  reset() {
    this.form.reset({
      invoiceId: '',
      paymentDate: new Date(),
      amount: 25000,
      method: 'CASH',
      reference: '',
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

type PaymentRow = Payment & { invoiceNumber: string };

function attachInvoiceNumbers(payments: Payment[], invoices: Invoice[]): PaymentRow[] {
  const invoiceNumbers = new Map(invoices.map((invoice) => [invoice.id, invoice.invoiceNumber]));
  return payments.map((payment) => ({
    ...payment,
    invoiceNumber: invoiceNumbers.get(payment.invoiceId) ?? payment.invoiceId,
  }));
}


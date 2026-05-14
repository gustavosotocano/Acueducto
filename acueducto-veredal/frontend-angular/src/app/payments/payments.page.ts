import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  map,
  of,
  shareReplay,
  startWith,
  switchMap,
  take,
  tap,
} from 'rxjs';
import { Invoice, InvoicesService } from '../invoices/invoices.service';
import { Owner, OwnersService } from '../owners/owners.service';
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
          RF-059: filtre por propietario, revise abonos y registre solo el saldo pendiente.
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
              <mat-label>Propietario</mat-label>
              <mat-select formControlName="ownerId">
                @for (owner of ownerOptions$ | async; track owner.id) {
                  <mat-option [value]="owner.id">{{ owner.fullName }}</mat-option>
                }
              </mat-select>
              <mat-hint>Primero elija el propietario para ver sus facturas con saldo.</mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full">
              <mat-label>Factura</mat-label>
              <mat-select formControlName="invoiceId" (selectionChange)="onInvoiceSelected($event.value)">
                @if (!form.controls.ownerId.value) {
                  <mat-option disabled>Seleccione un propietario.</mat-option>
                } @else if (payableInvoices$ | async; as invoices) {
                  @if (invoices.length === 0) {
                    <mat-option disabled>No hay facturas con saldo para este propietario.</mat-option>
                  } @else {
                    @for (invoice of invoices; track invoice.id) {
                      <mat-option [value]="invoice.id">
                        {{ invoice.invoiceNumber }} ·
                        {{ invoice.balanceAmount | currency : 'COP' : 'symbol' : '1.0-0' }}
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
              @if (form.controls.amount.hasError('max')) {
                <mat-error>El monto no puede superar el saldo pendiente.</mat-error>
              } @else {
                <mat-hint>Máximo el saldo pendiente de la factura.</mat-hint>
              }
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
            @if (!form.controls.ownerId.value) {
              <p class="empty-hint">Seleccione un propietario para ver sus pagos.</p>
            } @else if (payments.length === 0) {
              <p class="empty-hint">No hay pagos registrados para este propietario.</p>
            } @else {
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
            }
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
  private readonly ownersService = inject(OwnersService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly savingSubject = new BehaviorSubject<boolean>(false);
  protected readonly saving$ = this.savingSubject.asObservable();

  protected readonly columns = ['paymentDate', 'amount', 'method', 'invoiceNumber'];

  protected readonly form = this.fb.nonNullable.group({
    ownerId: ['', [Validators.required]],
    invoiceId: ['', [Validators.required]],
    paymentDate: [new Date(), [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    method: ['CASH', [Validators.required]],
    reference: [''],
    notes: [''],
  });

  private readonly ownerId$ = this.form.controls.ownerId.valueChanges.pipe(
    startWith(this.form.controls.ownerId.value),
    tap(() => {
      this.form.patchValue({ invoiceId: '', amount: 0 });
      this.syncAmountValidators(null);
    }),
  );

  private readonly invoiceId$ = this.form.controls.invoiceId.valueChanges.pipe(
    startWith(this.form.controls.invoiceId.value),
  );

  private readonly owners$ = this.ownersService.list().pipe(
    catchError((err) => {
      const msg = err?.error?.message || 'No se pudo cargar propietarios';
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      return of([] as Owner[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly ownerOptions$ = this.owners$.pipe(
    map((owners) => owners.slice().sort((a, b) => a.fullName.localeCompare(b.fullName))),
    startWith([] as Owner[]),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly payableInvoices$ = combineLatest([this.ownerId$, this.refresh$]).pipe(
    switchMap(([ownerId]) => {
      if (!ownerId) {
        return of([] as PayableInvoice[]);
      }
      return combineLatest([
        this.invoicesService.list({ ownerId }),
        this.paymentsService.list({ ownerId }),
      ]).pipe(map(([invoices, payments]) => buildPayableInvoices(invoices, payments)));
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly selectedInvoice$ = combineLatest([this.payableInvoices$, this.invoiceId$]).pipe(
    map(([invoices, invoiceId]) => invoices.find((invoice) => invoice.id === invoiceId) ?? null),
    tap((invoice) => this.syncAmountValidators(invoice?.balanceAmount ?? null)),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    this.selectedInvoice$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  protected readonly payments$ = combineLatest([this.ownerId$, this.refresh$]).pipe(
    switchMap(([ownerId]) => {
      if (!ownerId) {
        return of([] as PaymentRow[]);
      }
      return combineLatest([
        this.paymentsService.list({ ownerId }),
        this.invoicesService.list({ ownerId }),
      ]).pipe(map(([payments, invoices]) => attachInvoiceNumbers(payments, invoices)));
    }),
    catchError(() => of([] as PaymentRow[])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  onInvoiceSelected(invoiceId: string) {
    this.payableInvoices$
      .pipe(
        take(1),
        map((invoices) => invoices.find((invoice) => invoice.id === invoiceId)),
        tap((invoice) => {
          if (invoice) {
            this.form.patchValue({ amount: invoice.balanceAmount });
            this.syncAmountValidators(invoice.balanceAmount);
          }
        }),
      )
      .subscribe();
  }

  private syncAmountValidators(balance: number | null) {
    const amountControl = this.form.controls.amount;
    const validators = [Validators.required, Validators.min(0.01)];
    if (balance != null && balance > 0) {
      validators.push(Validators.max(balance));
    }
    amountControl.setValidators(validators);
    amountControl.updateValueAndValidity({ emitEvent: false });
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
      ownerId: '',
      invoiceId: '',
      paymentDate: new Date(),
      amount: 0,
      method: 'CASH',
      reference: '',
      notes: '',
    });
    this.syncAmountValidators(null);
  }
}

function toIsoDate(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

type PayableInvoice = Invoice & {
  paidAmount: number;
  balanceAmount: number;
};

type PaymentRow = Payment & { invoiceNumber: string };

function buildPayableInvoices(invoices: Invoice[], payments: Payment[]): PayableInvoice[] {
  const paidByInvoice = sumPaymentsByInvoice(payments);
  return invoices
    .filter((invoice) => invoice.status !== 'CANCELLED')
    .map((invoice) => {
      const paidAmount = paidByInvoice.get(invoice.id) ?? 0;
      const balanceAmount = Math.max(0, roundMoney(invoice.totalAmount - paidAmount));
      return { ...invoice, paidAmount, balanceAmount };
    })
    .filter((invoice) => invoice.balanceAmount > 0)
    .sort((a, b) => a.invoiceNumber.localeCompare(b.invoiceNumber));
}

function sumPaymentsByInvoice(payments: Payment[]): Map<string, number> {
  const paidByInvoice = new Map<string, number>();
  for (const payment of payments) {
    const current = paidByInvoice.get(payment.invoiceId) ?? 0;
    paidByInvoice.set(payment.invoiceId, roundMoney(current + payment.amount));
  }
  return paidByInvoice;
}

function attachInvoiceNumbers(payments: Payment[], invoices: Invoice[]): PaymentRow[] {
  const invoiceNumbers = new Map(invoices.map((invoice) => [invoice.id, invoice.invoiceNumber]));
  return payments.map((payment) => ({
    ...payment,
    invoiceNumber: invoiceNumbers.get(payment.invoiceId) ?? payment.invoiceId,
  }));
}

function roundMoney(value: number): number {
  return Math.round(value * 100) / 100;
}

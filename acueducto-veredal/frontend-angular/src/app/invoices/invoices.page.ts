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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, combineLatest, map, of, shareReplay, startWith, switchMap, tap } from 'rxjs';
import { Owner, OwnersService } from '../owners/owners.service';
import { PropertiesService, Property } from '../properties/properties.service';
import { WaterRight, WaterRightsService } from '../water-rights/water-rights.service';
import {
  BatchMonthlyInvoiceRequest,
  BatchMonthlyInvoiceResponse,
  CreateInvoiceRequest,
  InvoicesService,
} from './invoices.service';

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
    MatSelectModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 style="margin: 0">Invoices</h2>
        <p style="margin: 4px 0 0; color: var(--mat-sys-on-surface-variant)">
          Factura manual, lote mensual (RF-060) y listado.
        </p>
      </div>
    </div>

    <mat-card class="card batch-card">
      <mat-card-header>
        <mat-card-title>Facturación mensual por lote</mat-card-title>
        <mat-card-subtitle>
          Genera facturas del mes para todos los derechos de agua en estado activo del tenant. Las omitidas no duplican
          periodo ya facturado.
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="batchForm" class="form batch-form" (ngSubmit)="submitBatch()">
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
            Fechas opcionales: si las deja vacías, el backend usa fin de mes como emisión y +15 días para vencimiento.
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
            <p style="margin: 12px 0 8px">
              <strong>Resultado:</strong> creadas {{ lastBatch.createdCount }}, omitidas {{ lastBatch.skippedCount }}.
              Periodo {{ lastBatch.periodStart }} → {{ lastBatch.periodEnd }}, emisión {{ lastBatch.issueDate }}, vence
              {{ lastBatch.dueDate }}.
            </p>
            @if (lastBatch.skipped.length > 0) {
              <table mat-table [dataSource]="lastBatch.skipped" class="mat-elevation-z0" style="width: 100%">
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

    <div class="grid">
      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Nueva factura</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" class="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>Propietario</mat-label>
              <mat-select formControlName="ownerId">
                @for (o of ownerOptions$ | async; track o.id) {
                  <mat-option [value]="o.id">{{ o.fullName }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Water right</mat-label>
              <mat-select formControlName="waterRightId">
                @for (wr of waterRightOptions$ | async; track wr.id) {
                  <mat-option [value]="wr.id">{{ wr.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Código water right</mat-label>
              <input matInput formControlName="waterRightCode" readonly />
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
  private readonly waterRightsService = inject(WaterRightsService);
  private readonly ownersService = inject(OwnersService);
  private readonly propertiesService = inject(PropertiesService);

  protected readonly months = Array.from({ length: 12 }, (_, i) => i + 1);
  protected readonly skipColumns = ['waterRightId', 'reason'];
  protected lastBatch: BatchMonthlyInvoiceResponse | null = null;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly savingSubject = new BehaviorSubject<boolean>(false);
  protected readonly saving$ = this.savingSubject.asObservable();

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

  protected readonly columns = ['invoiceNumber', 'status', 'issueDate', 'dueDate', 'totalAmount'];

  protected readonly invoices$ = this.refresh$.pipe(
    switchMap(() => this.invoicesService.list()),
    catchError(() => of([])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly form = this.fb.nonNullable.group({
    ownerId: ['', [Validators.required]],
    waterRightId: ['', [Validators.required]],
    waterRightCode: [{ value: '', disabled: false }],
    invoiceNumber: ['', [Validators.required]],
    periodStart: [new Date(), [Validators.required]],
    periodEnd: [new Date(), [Validators.required]],
    issueDate: [new Date(), [Validators.required]],
    dueDate: [new Date(), [Validators.required]],
    totalAmount: [25000, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  private readonly waterRights$ = this.waterRightsService.list().pipe(
    catchError((err) => {
      const msg = err?.error?.message || 'No se pudo cargar water rights';
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      return of([] as WaterRight[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  private readonly owners$ = this.ownersService.list().pipe(
    catchError((err) => {
      const msg = err?.error?.message || 'No se pudo cargar owners';
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      return of([] as Owner[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  private readonly properties$ = this.propertiesService.list().pipe(
    catchError((err) => {
      const msg = err?.error?.message || 'No se pudo cargar properties';
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      return of([] as Property[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  private readonly ownerId$ = this.form.controls.ownerId.valueChanges.pipe(
    startWith(this.form.controls.ownerId.value),
  );
  private readonly waterRightId$ = this.form.controls.waterRightId.valueChanges.pipe(
    startWith(this.form.controls.waterRightId.value),
  );

  protected readonly ownerOptions$ = this.owners$.pipe(
    map((owners) => owners.slice().sort((a, b) => a.fullName.localeCompare(b.fullName))),
    startWith([] as Owner[]),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly waterRightOptions$ = combineLatest([this.waterRights$, this.properties$, this.ownerId$]).pipe(
    map(([waterRights, properties, ownerId]) => {
      const propertyById = new Map(properties.map((p) => [p.id, p] as const));
      const filtered: WaterRight[] = ownerId ? waterRights.filter((wr) => wr.ownerId === ownerId) : [];
      return filtered
        .map((wr) => {
          const p = propertyById.get(wr.propertyId);
          const label = p?.propertyName || p?.propertyCode || wr.propertyId || wr.id;
          return { id: wr.id, label };
        })
        .sort((a, b) => a.label.localeCompare(b.label));
    }),
    tap((opts) => {
      // If current selection is no longer valid, clear it.
      const currentId = this.form.controls.waterRightId.value;
      if (!currentId) return;
      if (!opts.some((o) => o.id === currentId)) {
        this.form.controls.waterRightId.setValue('');
        this.form.controls.waterRightCode.setValue('');
      }
    }),
    startWith([] as { id: string; label: string }[]),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    // Copy selected waterRightId into the read-only field.
    this.waterRightId$
      .pipe(tap((id) => this.form.controls.waterRightCode.setValue(id, { emitEvent: false })))
      .subscribe();
  }

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
          this.snackBar.open(`Lote: ${res.createdCount} creadas, ${res.skippedCount} omitidas`, 'OK', { duration: 5000 });
        }),
        tap(() => this.refresh$.next()),
        catchError((err) => {
          const msg = err?.error?.message || 'Error en lote mensual';
          this.snackBar.open(msg, 'OK', { duration: 6000 });
          return of(null);
        }),
        tap(() => this.batchSavingSubject.next(false)),
      )
      .subscribe();
  }

  reset() {
    this.form.reset({
      ownerId: '',
      waterRightId: '',
      waterRightCode: '',
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


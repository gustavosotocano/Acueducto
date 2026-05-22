import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
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
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { toIsoDate } from './invoices-date.util';
import {
  ChangeInvoiceStatusRequest,
  CreateInvoiceRequest,
  Invoice,
  InvoicesService,
} from './invoices.service';

const INVOICE_STATUSES = ['DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'CANCELLED'] as const;

@Component({
  selector: 'app-invoices-manual-page',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatCardModule,
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
    MatDialogModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header
      title="Nueva factura"
      subtitle="Listado de facturas manuales. Crear desde el botón +; cambiar estado según RF-053 (sin editar datos emitidos, RF-056)."
    >
      <div actions>
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Nueva factura">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar listado">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </app-page-header>

    <mat-card class="card">
      <mat-card-header>
        <mat-card-title>Listado</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (invoices$ | async; as invoices) {
          <table mat-table [dataSource]="invoices" class="mat-elevation-z0 list-table">
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

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 72px">Acciones</th>
              <td mat-cell *matCellDef="let r" class="actions-cell">
                <button
                  mat-icon-button
                  type="button"
                  (click)="openChangeStatus(r)"
                  aria-label="Cambiar estado de factura"
                >
                  <mat-icon>edit</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columns"></tr>
            <tr mat-row *matRowDef="let row; columns: columns"></tr>
          </table>
        } @else {
          <div class="loading">
            <mat-spinner diameter="28" />
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './invoices-manual.page.scss',
})
export class InvoicesManualPage {
  private readonly invoicesService = inject(InvoicesService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  protected readonly columns = ['invoiceNumber', 'status', 'issueDate', 'dueDate', 'totalAmount', 'actions'];

  protected readonly invoices$ = this.refresh$.pipe(
    switchMap(() => this.invoicesService.list()),
    catchError((err) => {
      const msg = err?.error?.message || 'No se pudo cargar facturas';
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      return of([] as Invoice[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  refresh() {
    this.refresh$.next();
  }

  openCreate() {
    const ref = this.dialog.open(InvoiceCreateDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
    });

    ref.afterClosed().subscribe((payload: CreateInvoiceRequest | undefined) => {
      if (!payload) return;

      this.invoicesService
        .create(payload)
        .pipe(
          tap(() => this.snackBar.open('Factura creada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando factura';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openChangeStatus(invoice: Invoice) {
    const ref = this.dialog.open(InvoiceStatusDialogComponent, {
      width: '480px',
      maxWidth: '95vw',
      data: { invoice },
    });

    ref.afterClosed().subscribe((payload: ChangeInvoiceStatusRequest | undefined) => {
      if (!payload) return;

      this.invoicesService
        .changeStatus(invoice.id, payload)
        .pipe(
          tap(() => this.snackBar.open('Estado actualizado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error cambiando estado';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

@Component({
  selector: 'app-invoice-create-dialog',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  template: `
    <h2 mat-dialog-title>Nueva factura</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
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

        <mat-form-field appearance="outline" class="full">
          <mat-label>Notas</mat-label>
          <textarea matInput rows="3" formControlName="notes"></textarea>
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        <mat-icon>add</mat-icon>
        <span>Crear</span>
      </button>
    </div>
  `,
  styles: `
    .dialog-form {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      align-items: start;
    }
    .full {
      grid-column: 1 / -1;
    }
    @media (max-width: 720px) {
      .dialog-form {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class InvoiceCreateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly waterRightsService = inject(WaterRightsService);
  private readonly ownersService = inject(OwnersService);
  private readonly propertiesService = inject(PropertiesService);
  private readonly dialogRef = inject(MatDialogRef<InvoiceCreateDialogComponent>);

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
      const currentId = this.form.controls.waterRightId.value;
      if (!currentId) return;
      if (!opts.some((o) => o.id === currentId)) {
        this.form.controls.waterRightId.setValue('');
        this.form.controls.waterRightCode.setValue('');
      }
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  constructor() {
    this.waterRightId$
      .pipe(tap((id) => this.form.controls.waterRightCode.setValue(id, { emitEvent: false })))
      .subscribe();
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
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
    this.dialogRef.close(payload);
  }
}

@Component({
  selector: 'app-invoice-status-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Cambiar estado</h2>
    <div mat-dialog-content>
      <p class="invoice-ref">Factura <strong>{{ data.invoice.invoiceNumber }}</strong> · estado actual: {{ data.invoice.status }}</p>
      <form [formGroup]="form" class="status-form">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Nuevo estado</mat-label>
          <mat-select formControlName="status">
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Nota / motivo (opcional)</mat-label>
          <textarea matInput rows="3" formControlName="note" maxlength="250"></textarea>
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">Guardar</button>
    </div>
  `,
  styles: `
    .invoice-ref {
      margin: 0 0 12px;
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
    }
    .status-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .full {
      width: 100%;
    }
  `,
})
export class InvoiceStatusDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly data = inject<{ invoice: Invoice }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<InvoiceStatusDialogComponent>);

  protected readonly statuses = INVOICE_STATUSES;

  protected readonly form = this.fb.nonNullable.group({
    status: [this.data.invoice.status, [Validators.required]],
    note: [''],
  });

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    if (v.status === this.data.invoice.status && !v.note.trim()) {
      this.close();
      return;
    }
    this.dialogRef.close({
      status: v.status,
      note: v.note.trim() || null,
    } satisfies ChangeInvoiceStatusRequest);
  }
}

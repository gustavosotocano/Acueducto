import { AsyncPipe, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import {
  ConnectionTariff,
  ConnectionTariffsService,
  CreateConnectionTariffRequest,
  UpdateConnectionTariffRequest,
} from './connection-tariffs.service';

export const CONNECTION_TYPES = [
  'RESIDENTIAL',
  'COMMERCIAL',
  'AGRICULTURAL',
  'SHARED',
  'METERED',
  'UNMETERED',
] as const;

@Component({
  selector: 'app-connection-tariffs-page',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Tarifas por conexión" subtitle="Vigencia por tipo RESIDENTIAL, COMMERCIAL, etc. (RF-065).">
      <div actions>
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Nueva tarifa">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </app-page-header>

    <mat-card>
      <mat-card-content>
        @if (rows$ | async; as rows) {
          <table mat-table [dataSource]="rows" class="mat-elevation-z0" style="width: 100%">
            <ng-container matColumnDef="connectionType">
              <th mat-header-cell *matHeaderCellDef>Tipo</th>
              <td mat-cell *matCellDef="let r">{{ r.connectionType }}</td>
            </ng-container>

            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef>Valor</th>
              <td mat-cell *matCellDef="let r">{{ r.amount | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
            </ng-container>

            <ng-container matColumnDef="validFrom">
              <th mat-header-cell *matHeaderCellDef>Vigente desde</th>
              <td mat-cell *matCellDef="let r">{{ r.validFrom | date : 'yyyy-MM-dd' }}</td>
            </ng-container>

            <ng-container matColumnDef="validTo">
              <th mat-header-cell *matHeaderCellDef>Vigente hasta</th>
              <td mat-cell *matCellDef="let r">{{ r.validTo ? (r.validTo | date : 'yyyy-MM-dd') : '—' }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 100px">Acciones</th>
              <td mat-cell *matCellDef="let r" style="text-align: right">
                <button mat-icon-button type="button" (click)="openUpdate(r)" aria-label="Editar">
                  <mat-icon>edit</mat-icon>
                </button>
              </td>
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
  `,
  styleUrl: './connection-tariffs.page.scss',
})
export class ConnectionTariffsPage {
  private readonly service = inject(ConnectionTariffsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly columns = ['connectionType', 'amount', 'validFrom', 'validTo', 'actions'];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  protected readonly rows$ = this.refresh$.pipe(
    switchMap(() =>
      this.service.list().pipe(
        catchError((err) => {
          const msg = err?.error?.message || 'No se pudo cargar tarifas';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of([] as ConnectionTariff[]);
        }),
      ),
    ),
    map((items) =>
      items.slice().sort((a, b) => {
        const c = a.connectionType.localeCompare(b.connectionType);
        return c !== 0 ? c : b.validFrom.localeCompare(a.validFrom);
      }),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  refresh() {
    this.refresh$.next();
  }

  openCreate() {
    const ref = this.dialog.open(ConnectionTariffUpsertDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      data: { mode: 'create' as const },
    });
    ref.afterClosed().subscribe((payload: CreateConnectionTariffRequest | undefined) => {
      if (!payload) return;
      this.service
        .create(payload)
        .pipe(
          tap(() => this.snackBar.open('Tarifa creada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando tarifa';
            this.snackBar.open(msg, 'OK', { duration: 5000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openUpdate(row: ConnectionTariff) {
    const ref = this.dialog.open(ConnectionTariffUpsertDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      data: { mode: 'update' as const, row },
    });
    ref.afterClosed().subscribe((res: { id: string; payload: UpdateConnectionTariffRequest } | undefined) => {
      if (!res) return;
      this.service
        .update(res.id, res.payload)
        .pipe(
          tap(() => this.snackBar.open('Tarifa actualizada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error actualizando tarifa';
            this.snackBar.open(msg, 'OK', { duration: 5000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

@Component({
  selector: 'app-connection-tariff-upsert-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Nueva tarifa' : 'Editar tarifa' }}</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="form">
        @if (data.mode === 'create') {
          <mat-form-field appearance="outline" class="full">
            <mat-label>Tipo de conexión</mat-label>
            <mat-select formControlName="connectionType">
              @for (t of connectionTypes; track t) {
                <mat-option [value]="t">{{ t }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        } @else {
          <p style="margin: 0 0 8px"><strong>Tipo:</strong> {{ data.row.connectionType }}</p>
        }

        <mat-form-field appearance="outline" class="full">
          <mat-label>Valor (COP)</mat-label>
          <input matInput type="number" formControlName="amount" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Vigente desde</mat-label>
          <input matInput [matDatepicker]="df" formControlName="validFrom" />
          <mat-datepicker-toggle matIconSuffix [for]="df" />
          <mat-datepicker #df />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Vigente hasta (opcional)</mat-label>
          <input matInput [matDatepicker]="dt" formControlName="validTo" />
          <mat-datepicker-toggle matIconSuffix [for]="dt" />
          <mat-datepicker #dt />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        <mat-icon>{{ data.mode === 'create' ? 'add' : 'save' }}</mat-icon>
        <span>{{ data.mode === 'create' ? 'Crear' : 'Guardar' }}</span>
      </button>
    </div>
  `,
  styleUrl: './connection-tariffs.page.scss',
})
export class ConnectionTariffUpsertDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ConnectionTariffUpsertDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<{ mode: 'create' } | { mode: 'update'; row: ConnectionTariff }>(MAT_DIALOG_DATA);

  protected readonly connectionTypes = [...CONNECTION_TYPES];

  protected readonly form = this.fb.nonNullable.group({
    connectionType: ['RESIDENTIAL', [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0)]],
    validFrom: [new Date(), [Validators.required]],
    validTo: [null as Date | null],
  });

  constructor() {
    if (this.data.mode === 'update') {
      const r = this.data.row;
      this.form.patchValue({
        amount: r.amount,
        validFrom: parseYmd(r.validFrom),
        validTo: r.validTo ? parseYmd(r.validTo) : null,
      });
      this.form.controls.connectionType.disable();
    }
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const from = toIsoDate(v.validFrom);
    const to = v.validTo ? toIsoDate(v.validTo) : null;
    if (to && to < from) {
      this.snackBar.open('La fecha fin no puede ser anterior al inicio', 'OK', { duration: 4000 });
      return;
    }
    if (this.data.mode === 'create') {
      const payload: CreateConnectionTariffRequest = {
        connectionType: v.connectionType,
        amount: Number(v.amount),
        validFrom: from,
        validTo: to,
      };
      this.dialogRef.close(payload);
      return;
    }
    const payload: UpdateConnectionTariffRequest = {
      amount: Number(v.amount),
      validFrom: from,
      validTo: to,
    };
    this.dialogRef.close({ id: this.data.row.id, payload });
  }
}

function parseYmd(s: string): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function toIsoDate(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

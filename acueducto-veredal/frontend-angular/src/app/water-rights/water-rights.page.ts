import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, combineLatest, map, of, shareReplay, startWith, switchMap, tap } from 'rxjs';
import { OwnersService } from '../owners/owners.service';
import { PropertiesService } from '../properties/properties.service';
import {
  CreateWaterRightRequest,
  DeactivateWaterRightRequest,
  ConnectionType,
  UpdateWaterRightRequest,
  WaterRight,
  WaterRightsService,
} from './water-rights.service';

@Component({
  selector: 'app-water-rights-page',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    ReactiveFormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 class="header-title">Water rights</h2>
        <p class="header-subtitle">
          Listado de derechos de agua del tenant actual.
        </p>
      </div>
      <div class="header-actions">
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Agregar water right">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar listado">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </div>

    @if (vm$ | async; as vm) {
      <div class="filters">
        <mat-form-field appearance="outline">
          <mat-label>Búsqueda</mat-label>
          <input matInput placeholder="Código / Owner / Property" [formControl]="filters.controls.q" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Estado</mat-label>
          <mat-select [formControl]="filters.controls.status">
            <mat-option value="">Todos</mat-option>
            @for (s of vm.statusOptions; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <button mat-stroked-button type="button" (click)="clearFilters()">
          <mat-icon>filter_alt_off</mat-icon>
          Limpiar
        </button>
      </div>

      <table
        mat-table
        [dataSource]="vm.items"
        class="mat-elevation-z1"
        style="width: 100%"
      >
        <ng-container matColumnDef="rightCode">
          <th mat-header-cell *matHeaderCellDef>Código</th>
          <td mat-cell *matCellDef="let r">{{ r.rightCode }}</td>
        </ng-container>

        <ng-container matColumnDef="serviceStatus">
          <th mat-header-cell *matHeaderCellDef>Estado</th>
          <td mat-cell *matCellDef="let r">{{ r.serviceStatus }}</td>
        </ng-container>

        <ng-container matColumnDef="connectionType">
          <th mat-header-cell *matHeaderCellDef>Tipo</th>
          <td mat-cell *matCellDef="let r">{{ r.connectionType }}</td>
        </ng-container>

        <ng-container matColumnDef="baseTariff">
          <th mat-header-cell *matHeaderCellDef>Tarifa base</th>
          <td mat-cell *matCellDef="let r">{{ r.baseTariff | currency : 'COP' : 'symbol' : '1.0-0' }}</td>
        </ng-container>

        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef style="width: 120px">Acciones</th>
          <td mat-cell *matCellDef="let r" class="actions-cell">
            <button mat-icon-button type="button" (click)="openUpdate(r)" aria-label="Editar derecho">
              <mat-icon>edit</mat-icon>
            </button>
            <button mat-icon-button type="button" (click)="openDeactivate(r)" aria-label="Borrar derecho">
              <mat-icon>delete</mat-icon>
            </button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
      </table>
    } @else {
      <div style="display: grid; place-items: center; height: 240px">
        <mat-spinner diameter="28" />
      </div>
    }
  `,
  styles: `
    .header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .header-title {
      margin: 0;
      line-height: 1.25;
    }
    .header-subtitle {
      margin: 4px 0 0;
      line-height: 1.25;
      color: var(--mat-sys-on-surface-variant);
    }
    .header-actions {
      display: flex;
      gap: 4px;
      align-items: center;
      margin-top: 2px;
    }
    .filters {
      display: grid;
      grid-template-columns: 1fr 240px auto;
      gap: 12px;
      align-items: center;
      margin: 0 0 12px;
    }
    .actions-cell {
      display: flex;
      gap: 4px;
      justify-content: flex-end;
      align-items: center;
      height: 51px;
    }
    @media (max-width: 900px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class WaterRightsPage {
  private readonly service = inject(WaterRightsService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly filters = this.fb.nonNullable.group({
    q: [''],
    status: [''],
  });

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly items$ = this.refresh$.pipe(
    switchMap(() =>
      this.service.list().pipe(
        catchError((err) => {
          const msg = err?.error?.message || 'No se pudo cargar water rights';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of([]);
        }),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly vm$ = combineLatest([
    this.items$,
    this.filters.valueChanges.pipe(startWith(this.filters.getRawValue())),
  ]).pipe(
    map(([items, filters]) => {
      const q = (filters.q ?? '').trim().toLowerCase();
      const status = (filters.status ?? '').trim().toLowerCase();

      const statusOptions = Array.from(new Set(items.map((i) => i.serviceStatus).filter(Boolean))).sort();

      const filtered = items.filter((i) => {
        const matchesQ =
          !q ||
          i.rightCode.toLowerCase().includes(q) ||
          i.ownerId.toLowerCase().includes(q) ||
          i.propertyId.toLowerCase().includes(q);
        const matchesStatus = !status || i.serviceStatus.toLowerCase() === status;
        return matchesQ && matchesStatus;
      });

      return { items: filtered, statusOptions };
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly displayedColumns = ['rightCode', 'serviceStatus', 'connectionType', 'baseTariff', 'actions'];

  clearFilters() {
    this.filters.reset({ q: '', status: '' });
  }

  refresh() {
    this.refresh$.next();
  }

  openUpdate(r: WaterRight) {
    const ref = this.dialog.open(WaterRightUpdateDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      data: { right: r },
    });

    ref.afterClosed().subscribe((payload: UpdateWaterRightRequest | undefined) => {
      if (!payload) return;
      this.service
        .update(r.id, payload)
        .pipe(
          tap(() => this.snackBar.open('Derecho actualizado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error actualizando derecho';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openCreate() {
    const ref = this.dialog.open(WaterRightCreateDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
    });

    ref.afterClosed().subscribe((payload: CreateWaterRightRequest | undefined) => {
      if (!payload) return;
      this.service
        .create(payload)
        .pipe(
          tap(() => this.snackBar.open('Derecho creado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando derecho';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openDeactivate(r: WaterRight) {
    const ref = this.dialog.open(WaterRightDeactivateDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      data: { right: r },
    });

    ref.afterClosed().subscribe((payload: DeactivateWaterRightRequest | undefined) => {
      if (!payload) return;
      this.service
        .deactivate(r.id, payload)
        .pipe(
          tap(() => this.snackBar.open('Derecho desactivado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error desactivando derecho';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

const CONNECTION_TYPES: { value: ConnectionType; label: string }[] = [
  { value: 'RESIDENTIAL', label: 'RESIDENTIAL' },
  { value: 'COMMERCIAL', label: 'COMMERCIAL' },
  { value: 'AGRICULTURAL', label: 'AGRICULTURAL' },
  { value: 'SHARED', label: 'SHARED' },
  { value: 'METERED', label: 'METERED' },
  { value: 'UNMETERED', label: 'UNMETERED' },
];

@Component({
  selector: 'app-water-right-create-dialog',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Nuevo water right</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Código</mat-label>
          <input matInput formControlName="rightCode" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Owner</mat-label>
          <mat-select formControlName="ownerId">
            @for (o of owners$ | async; track o.id) {
              <mat-option [value]="o.id">{{ o.fullName }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Property</mat-label>
          <mat-select formControlName="propertyId">
            @for (p of properties$ | async; track p.id) {
              <mat-option [value]="p.id">{{ p.propertyCode }} - {{ p.propertyName }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Water meter ID (opcional)</mat-label>
          <input matInput formControlName="waterMeterId" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Estado</mat-label>
          <mat-select formControlName="serviceStatus">
            <mat-option value="ACTIVE">ACTIVE</mat-option>
            <mat-option value="INACTIVE">INACTIVE</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Tipo conexión</mat-label>
          <mat-select formControlName="connectionType">
            @for (ct of connectionTypes; track ct.value) {
              <mat-option [value]="ct.value">{{ ct.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Tarifa base</mat-label>
          <input matInput type="number" formControlName="baseTariff" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Fecha inicio</mat-label>
          <input matInput type="date" formControlName="ownershipStartDate" />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="create()">
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
    @media (max-width: 720px) {
      .dialog-form {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class WaterRightCreateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<WaterRightCreateDialogComponent>);
  private readonly ownersService = inject(OwnersService);
  private readonly propertiesService = inject(PropertiesService);

  protected readonly owners$ = this.ownersService.list();
  protected readonly properties$ = this.propertiesService.list();

  protected readonly form = this.fb.nonNullable.group({
    rightCode: ['', [Validators.required, Validators.maxLength(50)]],
    ownerId: ['', [Validators.required]],
    propertyId: ['', [Validators.required]],
    waterMeterId: [''],
    serviceStatus: ['ACTIVE', [Validators.required, Validators.maxLength(20)]],
    connectionType: ['RESIDENTIAL' as ConnectionType, [Validators.required]],
    baseTariff: [0, [Validators.required, Validators.min(0)]],
    ownershipStartDate: ['', [Validators.required]],
  });

  protected readonly connectionTypes = CONNECTION_TYPES;

  constructor() {
    // Back-end expects a rightCode; autogenerate a UUID-like code for convenience.
    const uuid =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}`;
    this.form.controls.rightCode.setValue(uuid);
  }

  close() {
    this.dialogRef.close();
  }

  create() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const payload: CreateWaterRightRequest = {
      rightCode: v.rightCode.trim(),
      ownerId: v.ownerId.trim(),
      propertyId: v.propertyId.trim(),
      waterMeterId: v.waterMeterId?.trim() || null,
      serviceStatus: v.serviceStatus.trim(),
      connectionType: v.connectionType,
      baseTariff: Number(v.baseTariff),
      ownershipStartDate: v.ownershipStartDate,
    };
    this.dialogRef.close(payload);
  }
}

@Component({
  selector: 'app-water-right-update-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Actualizar water right</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Property ID</mat-label>
          <input matInput formControlName="propertyId" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Water meter ID (opcional)</mat-label>
          <input matInput formControlName="waterMeterId" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Estado</mat-label>
          <mat-select formControlName="serviceStatus">
            <mat-option value="ACTIVE">ACTIVE</mat-option>
            <mat-option value="INACTIVE">INACTIVE</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Tipo conexión</mat-label>
          <mat-select formControlName="connectionType">
            @for (ct of connectionTypes; track ct.value) {
              <mat-option [value]="ct.value">{{ ct.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Tarifa base</mat-label>
          <input matInput type="number" formControlName="baseTariff" />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        <mat-icon>save</mat-icon>
        <span>Guardar</span>
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
export class WaterRightUpdateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<WaterRightUpdateDialogComponent>);
  protected readonly data = inject<{ right: WaterRight }>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    propertyId: ['', [Validators.required]],
    waterMeterId: [''],
    serviceStatus: ['ACTIVE', [Validators.required, Validators.maxLength(20)]],
    connectionType: ['RESIDENTIAL' as ConnectionType, [Validators.required]],
    baseTariff: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly connectionTypes = CONNECTION_TYPES;

  constructor() {
    const r = this.data.right;
    this.form.patchValue({
      propertyId: r.propertyId,
      waterMeterId: r.waterMeterId || '',
      serviceStatus: r.serviceStatus,
      connectionType: r.connectionType,
      baseTariff: r.baseTariff,
    });
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const payload: UpdateWaterRightRequest = {
      propertyId: v.propertyId.trim(),
      waterMeterId: v.waterMeterId?.trim() || null,
      serviceStatus: v.serviceStatus.trim(),
      connectionType: v.connectionType,
      baseTariff: Number(v.baseTariff),
    };
    this.dialogRef.close(payload);
  }
}

@Component({
  selector: 'app-water-right-deactivate-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Desactivar water right</h2>
    <div mat-dialog-content>
      <p style="margin-top: 0">
        Vas a desactivar <strong>{{ data.right.rightCode }}</strong>. Esto requiere fecha fin de propiedad.
      </p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" style="width: 100%">
          <mat-label>Fecha fin</mat-label>
          <input matInput type="date" formControlName="ownershipEndDate" />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="warn" type="button" [disabled]="form.invalid" (click)="deactivate()">
        Desactivar
      </button>
    </div>
  `,
})
export class WaterRightDeactivateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<WaterRightDeactivateDialogComponent>);
  protected readonly data = inject<{ right: WaterRight }>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    ownershipEndDate: ['', [Validators.required]],
  });

  close() {
    this.dialogRef.close();
  }

  deactivate() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.dialogRef.close({ ownershipEndDate: v.ownershipEndDate } satisfies DeactivateWaterRightRequest);
  }
}


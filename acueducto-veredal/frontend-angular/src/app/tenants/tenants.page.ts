import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { CreateTenantRequest, Tenant, TenantsService, UpdateTenantRequest } from './tenants.service';

@Component({
  selector: 'app-tenants-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header title="Tenants" subtitle="Administración de acueductos veredales.">
      <div actions>
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Agregar tenant">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar listado">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </app-page-header>

    <mat-card>
      <mat-card-content>
        @if (tenants$ | async; as tenants) {
          <table mat-table [dataSource]="tenants" class="mat-elevation-z0" style="width: 100%">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>ID</th>
              <td mat-cell *matCellDef="let t">{{ t.id }}</td>
            </ng-container>

            <ng-container matColumnDef="tenantName">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let t">{{ t.tenantName }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let t">{{ t.status }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 140px">Acciones</th>
              <td mat-cell *matCellDef="let t" style="display:flex; gap:4px; justify-content:flex-end;">
                <button mat-icon-button type="button" (click)="openUpdate(t)" aria-label="Editar tenant">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button type="button" (click)="deactivate(t)" aria-label="Desactivar tenant">
                  <mat-icon>delete</mat-icon>
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
})
export class TenantsPage {
  private readonly service = inject(TenantsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly columns = ['id', 'tenantName', 'status', 'actions'];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  protected readonly tenants$ = this.refresh$.pipe(
    switchMap(() =>
      this.service.list().pipe(
        catchError((err) => {
          const msg = err?.error?.message || 'No se pudo cargar tenants';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of([] as Tenant[]);
        }),
      ),
    ),
    map((items) => items.slice().sort((a, b) => a.tenantName.localeCompare(b.tenantName))),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  refresh() {
    this.refresh$.next();
  }

  openCreate() {
    const ref = this.dialog.open(TenantUpsertDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'create' as const },
    });

    ref.afterClosed().subscribe((payload: CreateTenantRequest | undefined) => {
      if (!payload) return;
      this.service
        .create(payload)
        .pipe(
          tap(() => this.snackBar.open('Tenant creado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando tenant';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openUpdate(t: Tenant) {
    const ref = this.dialog.open(TenantUpsertDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'update' as const, tenant: t },
    });

    ref.afterClosed().subscribe((res: { id: string; payload: UpdateTenantRequest } | undefined) => {
      if (!res) return;
      this.service
        .update(res.id, res.payload)
        .pipe(
          tap(() => this.snackBar.open('Tenant actualizado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error actualizando tenant';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  deactivate(t: Tenant) {
    this.service
      .deactivate(t.id)
      .pipe(
        tap(() => this.snackBar.open('Tenant desactivado', 'OK', { duration: 2500 })),
        tap(() => this.refresh()),
        catchError((err) => {
          const msg = err?.error?.message || 'Error desactivando tenant';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of(null);
        }),
      )
      .subscribe();
  }
}

@Component({
  selector: 'app-tenant-upsert-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Nuevo tenant' : 'Actualizar tenant' }}</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" style="display:grid; grid-template-columns: 1fr 1fr; gap:12px">
        @if (data.mode === 'create') {
          <mat-form-field appearance="outline">
            <mat-label>ID</mat-label>
            <input matInput formControlName="id" placeholder="acueducto-demo" />
          </mat-form-field>
        }

        <mat-form-field appearance="outline" class="full">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="tenantName" placeholder="Acueducto Veredal ..." />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Contacto</mat-label>
          <input matInput formControlName="contactName" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Teléfono</mat-label>
          <input matInput formControlName="contactPhone" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="contactEmail" />
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
  styles: `
    .full { grid-column: 1 / -1; }
    @media (max-width: 720px) { form { grid-template-columns: 1fr !important; } }
  `,
})
export class TenantUpsertDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<TenantUpsertDialogComponent>);
  protected readonly data = inject<{ mode: 'create' } | { mode: 'update'; tenant: Tenant }>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    id: ['', [Validators.required, Validators.maxLength(64)]],
    tenantName: ['', [Validators.required, Validators.maxLength(200)]],
    contactName: ['', [Validators.maxLength(200)]],
    contactPhone: ['', [Validators.maxLength(30)]],
    contactEmail: ['', [Validators.email, Validators.maxLength(150)]],
  });

  constructor() {
    if (this.data.mode === 'update') {
      const t = this.data.tenant;
      this.form.patchValue({
        id: t.id,
        tenantName: t.tenantName,
        contactName: t.contactName ?? '',
        contactPhone: t.contactPhone ?? '',
        contactEmail: t.contactEmail ?? '',
      });
      this.form.controls.id.disable();
    }
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    if (this.data.mode === 'create') {
      const payload: CreateTenantRequest = {
        id: v.id.trim(),
        tenantName: v.tenantName.trim(),
        contactName: v.contactName?.trim() || null,
        contactPhone: v.contactPhone?.trim() || null,
        contactEmail: v.contactEmail?.trim() || null,
      };
      this.dialogRef.close(payload);
      return;
    }

    const payload: UpdateTenantRequest = {
      tenantName: v.tenantName.trim(),
      contactName: v.contactName?.trim() || null,
      contactPhone: v.contactPhone?.trim() || null,
      contactEmail: v.contactEmail?.trim() || null,
    };
    this.dialogRef.close({ id: this.data.tenant.id, payload });
  }
}


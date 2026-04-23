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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, of, shareReplay, switchMap, tap } from 'rxjs';
import { OwnersService } from '../owners/owners.service';
import { CreatePropertyRequest, PropertiesService, Property, UpdatePropertyRequest } from './properties.service';

@Component({
  selector: 'app-properties-page',
  imports: [
    AsyncPipe,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDialogModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 class="header-title">Properties</h2>
        <p class="header-subtitle">Predios/lugares físicos asociados al tenant.</p>
      </div>

      <div class="header-actions">
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Agregar property">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar listado">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </div>

    <mat-card class="card">
      <mat-card-content>
        @if (items$ | async; as items) {
          <table mat-table [dataSource]="items" class="mat-elevation-z0" style="width: 100%">
            <ng-container matColumnDef="propertyCode">
              <th mat-header-cell *matHeaderCellDef>Código</th>
              <td mat-cell *matCellDef="let p">{{ p.propertyCode }}</td>
            </ng-container>

            <ng-container matColumnDef="propertyName">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let p">{{ p.propertyName }}</td>
            </ng-container>

            <ng-container matColumnDef="addressLine">
              <th mat-header-cell *matHeaderCellDef>Dirección</th>
              <td mat-cell *matCellDef="let p">{{ p.addressLine }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let p">{{ p.status }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 120px">Acciones</th>
              <td mat-cell *matCellDef="let p" class="actions-cell">
                <button mat-icon-button type="button" (click)="openUpdate(p)" aria-label="Editar property">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button type="button" (click)="openDeactivate(p)" aria-label="Borrar property">
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
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './properties.page.scss',
})
export class PropertiesPage {
  private readonly service = inject(PropertiesService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  protected readonly items$ = this.refresh$.pipe(
    switchMap(() =>
      this.service.list().pipe(
        catchError((err) => {
          const msg = err?.error?.message || 'No se pudo cargar properties';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of([]);
        }),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly displayedColumns = ['propertyCode', 'propertyName', 'addressLine', 'status', 'actions'];

  refresh() {
    this.refresh$.next();
  }

  openCreate() {
    const ref = this.dialog.open(PropertyCreateDialogComponent, { width: '760px', maxWidth: '95vw' });
    ref.afterClosed().subscribe((payload: CreatePropertyRequest | undefined) => {
      if (!payload) return;
      this.service
        .create(payload)
        .pipe(
          tap(() => this.snackBar.open('Property creada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando property';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openUpdate(p: Property) {
    const ref = this.dialog.open(PropertyUpdateDialogComponent, { width: '720px', maxWidth: '95vw', data: { p } });
    ref.afterClosed().subscribe((payload: UpdatePropertyRequest | undefined) => {
      if (!payload) return;
      this.service
        .update(p.id, payload)
        .pipe(
          tap(() => this.snackBar.open('Property actualizada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error actualizando property';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openDeactivate(p: Property) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: {
        title: 'Desactivar property',
        message: `¿Deseas desactivar "${p.propertyCode}"?`,
        confirmText: 'Desactivar',
      },
    });
    ref.afterClosed().subscribe((ok: boolean | undefined) => {
      if (!ok) return;
      this.service
        .deactivate(p.id)
        .pipe(
          tap(() => this.snackBar.open('Property desactivada', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error desactivando property';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

@Component({
  selector: 'app-property-create-dialog',
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
    <h2 mat-dialog-title>Nueva property</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Código</mat-label>
          <input matInput formControlName="propertyCode" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Owner</mat-label>
          <mat-select formControlName="ownerId">
            @for (o of owners$ | async; track o.id) {
              <mat-option [value]="o.id">{{ o.fullName }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="propertyName" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Dirección</mat-label>
          <input matInput formControlName="addressLine" />
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
})
export class PropertyCreateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PropertyCreateDialogComponent>);
  private readonly ownersService = inject(OwnersService);

  protected readonly owners$ = this.ownersService.list();

  protected readonly form = this.fb.nonNullable.group({
    propertyCode: ['', [Validators.required, Validators.maxLength(50)]],
    propertyName: ['', [Validators.required, Validators.maxLength(200)]],
    addressLine: ['', [Validators.required, Validators.maxLength(250)]],
    ownerId: ['', [Validators.required]],
    ownershipStartDate: ['', [Validators.required]],
  });

  close() {
    this.dialogRef.close();
  }

  create() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const payload: CreatePropertyRequest = {
      propertyCode: v.propertyCode.trim(),
      propertyName: v.propertyName.trim(),
      addressLine: v.addressLine.trim(),
      ownerId: v.ownerId,
      ownershipStartDate: v.ownershipStartDate,
    };
    this.dialogRef.close(payload);
  }
}

@Component({
  selector: 'app-property-update-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Actualizar property</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="propertyName" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Dirección</mat-label>
          <input matInput formControlName="addressLine" />
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
})
export class PropertyUpdateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PropertyUpdateDialogComponent>);
  protected readonly data = inject<{ p: Property }>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.nonNullable.group({
    propertyName: ['', [Validators.required, Validators.maxLength(200)]],
    addressLine: ['', [Validators.required, Validators.maxLength(250)]],
  });

  constructor() {
    this.form.patchValue({
      propertyName: this.data.p.propertyName,
      addressLine: this.data.p.addressLine || '',
    });
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const payload: UpdatePropertyRequest = {
      propertyName: v.propertyName.trim(),
      addressLine: v.addressLine.trim(),
    };
    this.dialogRef.close(payload);
  }
}

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <div mat-dialog-content>{{ data.message }}</div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close(false)">Cancelar</button>
      <button mat-flat-button color="warn" type="button" (click)="close(true)">{{ data.confirmText ?? 'Confirmar' }}</button>
    </div>
  `,
  imports: [MatDialogModule, MatButtonModule],
})
export class ConfirmDialogComponent {
  protected readonly data = inject<{ title: string; message: string; confirmText?: string }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent>);
  close(v: boolean) {
    this.dialogRef.close(v);
  }
}


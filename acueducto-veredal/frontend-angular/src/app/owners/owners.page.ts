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
import { BehaviorSubject, catchError, of, shareReplay, switchMap, tap } from 'rxjs';
import { CreateOwnerRequest, Owner, OwnersService, UpdateOwnerRequest } from './owners.service';

@Component({
  selector: 'app-owners-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="header">
      <div>
        <h2 class="header-title">Owners</h2>
        <p class="header-subtitle">
          Crear propietarios y consultar listado del tenant.
        </p>
      </div>
      <div class="header-actions">
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Agregar owner">
          <mat-icon>add</mat-icon>
        </button>
        <button mat-icon-button type="button" (click)="refresh()" aria-label="Actualizar listado">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
    </div>

    <mat-card class="card">
      <mat-card-header>
        <mat-card-title>Listado</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (owners$ | async; as owners) {
          <table mat-table [dataSource]="owners" class="mat-elevation-z0" style="width: 100%">
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let o">{{ o.fullName }}</td>
            </ng-container>

            <ng-container matColumnDef="document">
              <th mat-header-cell *matHeaderCellDef>Documento</th>
              <td mat-cell *matCellDef="let o">{{ o.documentType }} {{ o.documentNumber }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let o">{{ o.status }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 120px">Acciones</th>
              <td mat-cell *matCellDef="let o" class="actions-cell">
                <button mat-icon-button type="button" (click)="openUpdate(o)" aria-label="Editar owner">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button type="button" (click)="openDeactivate(o)" aria-label="Borrar owner">
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
  styleUrl: './owners.page.scss',
})
export class OwnersPage {
  private readonly ownersService = inject(OwnersService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  protected readonly owners$ = this.refresh$.pipe(
    switchMap(() =>
      this.ownersService.list().pipe(
        catchError((err) => {
          const msg = err?.error?.message || 'No se pudo cargar owners';
          this.snackBar.open(msg, 'OK', { duration: 4000 });
          return of([]);
        }),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );
  protected readonly displayedColumns = ['fullName', 'document', 'status', 'actions'];

  refresh() {
    this.refresh$.next();
  }

  openCreate() {
    const ref = this.dialog.open(OwnerUpsertDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'create' as const },
    });

    ref.afterClosed().subscribe((res: OwnerUpsertDialogResult | undefined) => {
      if (!res || res.mode !== 'create') return;

      this.ownersService
        .create(res.payload)
        .pipe(
          tap(() => this.snackBar.open('Owner creado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error creando owner';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openUpdate(owner: Owner) {
    const ref = this.dialog.open(OwnerUpsertDialogComponent, {
      width: '720px',
      maxWidth: '95vw',
      data: { mode: 'update' as const, owner },
    });

    ref.afterClosed().subscribe((res: OwnerUpsertDialogResult | undefined) => {
      if (!res || res.mode !== 'update') return;

      this.ownersService
        .update(res.id, res.payload)
        .pipe(
          tap(() => this.snackBar.open('Owner actualizado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error actualizando owner';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openDeactivate(owner: Owner) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: {
        title: 'Desactivar owner',
        message: `¿Deseas desactivar a "${owner.fullName}"?`,
        confirmText: 'Desactivar',
      },
    });

    ref.afterClosed().subscribe((ok: boolean | undefined) => {
      if (!ok) return;

      this.ownersService
        .deactivate(owner.id)
        .pipe(
          tap(() => this.snackBar.open('Owner desactivado', 'OK', { duration: 2500 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message || 'Error desactivando owner';
            this.snackBar.open(msg, 'OK', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

type OwnerUpsertDialogResult =
  | { mode: 'create'; payload: CreateOwnerRequest }
  | { mode: 'update'; id: string; payload: UpdateOwnerRequest };

@Component({
  selector: 'app-owner-upsert-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Nuevo owner' : 'Actualizar owner' }}</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        @if (data.mode === 'create') {
          <mat-form-field appearance="outline">
            <mat-label>Tipo documento</mat-label>
            <input matInput formControlName="documentType" placeholder="CC / NIT" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Número</mat-label>
            <input matInput formControlName="documentNumber" />
          </mat-form-field>
        }

        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="firstName" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Primer apellido</mat-label>
          <input matInput formControlName="lastName" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Segundo apellido (opcional)</mat-label>
          <input matInput formControlName="secondLastName" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Teléfono</mat-label>
          <input matInput formControlName="phone" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        <mat-icon>{{ data.mode === 'create' ? 'person_add' : 'save' }}</mat-icon>
        <span>{{ data.mode === 'create' ? 'Crear' : 'Guardar' }}</span>
      </button>
    </div>
  `,
})
export class OwnerUpsertDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly data = inject<{ mode: 'create' } | { mode: 'update'; owner: Owner }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OwnerUpsertDialogComponent>);

  protected readonly form = this.fb.nonNullable.group({
    documentType: ['CC', [Validators.required, Validators.maxLength(30)]],
    documentNumber: ['', [Validators.required, Validators.maxLength(50)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    secondLastName: ['', [Validators.maxLength(100)]],
    phone: ['', [Validators.maxLength(30)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
  });

  constructor() {
    if (this.data.mode === 'update') {
      const o = this.data.owner;
      this.form.patchValue({
        documentType: o.documentType,
        documentNumber: o.documentNumber,
        firstName: o.firstName || '',
        lastName: o.lastName || '',
        secondLastName: o.secondLastName || '',
        phone: o.phone || '',
        email: o.email || '',
      });
      this.form.controls.documentType.disable();
      this.form.controls.documentNumber.disable();
    }
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();

    if (this.data.mode === 'create') {
      const payload: CreateOwnerRequest = {
        documentType: v.documentType.trim(),
        documentNumber: v.documentNumber.trim(),
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        secondLastName: v.secondLastName?.trim() || null,
        phone: v.phone?.trim() || null,
        email: v.email?.trim() || null,
      };
      this.dialogRef.close({ mode: 'create', payload } satisfies OwnerUpsertDialogResult);
      return;
    }

    const payload: UpdateOwnerRequest = {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      secondLastName: v.secondLastName?.trim() || null,
      phone: v.phone?.trim() || null,
      email: v.email?.trim() || null,
    };

    const id = this.data.owner.id;
    if (!id) {
      this.snackBar.open('No se pudo identificar el owner', 'OK', { duration: 3000 });
      return;
    }
    this.dialogRef.close({ mode: 'update', id, payload } satisfies OwnerUpsertDialogResult);
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


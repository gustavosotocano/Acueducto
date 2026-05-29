import { AsyncPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, of, shareReplay, switchMap, tap } from 'rxjs';
import { AuthStore } from '../core/auth/auth.store';
import { TenantStore } from '../core/tenant/tenant.store';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { Tenant, TenantsService } from '../tenants/tenants.service';
import { AppUser, CreateUserRequest, UpdateUserRequest, UsersService } from './users.service';

@Component({
  selector: 'app-users-page',
  imports: [
    AsyncPipe,
    MatCardModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDialogModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header [title]="'Usuarios'" [subtitle]="pageSubtitle()">
      <div actions>
        <button mat-icon-button type="button" (click)="openCreate()" aria-label="Nuevo usuario">
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
        @if (isPlatformAdmin()) {
          <mat-form-field appearance="outline" class="filter">
            <mat-label>Filtrar por acueducto</mat-label>
            <mat-select [value]="listTenantFilter()" (selectionChange)="onListFilterChange($event.value)">
              <mat-option value="">Todos</mat-option>
              @for (t of tenants(); track t.id) {
                <mat-option [value]="t.id">{{ t.tenantName }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
        @if (users$ | async; as users) {
          <table mat-table [dataSource]="users" class="mat-elevation-z0" style="width: 100%">
            @if (isPlatformAdmin()) {
              <ng-container matColumnDef="tenantId">
                <th mat-header-cell *matHeaderCellDef>Acueducto</th>
                <td mat-cell *matCellDef="let u">{{ tenantLabel(u.tenantId) }}</td>
              </ng-container>
            }
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let u">{{ u.fullName }}</td>
            </ng-container>
            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef>Correo</th>
              <td mat-cell *matCellDef="let u">{{ u.email }}</td>
            </ng-container>
            <ng-container matColumnDef="roleCode">
              <th mat-header-cell *matHeaderCellDef>Rol</th>
              <td mat-cell *matCellDef="let u">{{ u.roleCode }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let u">{{ u.status }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="width: 140px">Acciones</th>
              <td mat-cell *matCellDef="let u" class="actions-cell">
                @if (canManageRow(u)) {
                  <button mat-icon-button type="button" (click)="openUpdate(u)" aria-label="Editar usuario">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button
                    mat-icon-button
                    type="button"
                    (click)="openResetPassword(u)"
                    aria-label="Restablecer contraseña"
                  >
                    <mat-icon>lock_reset</mat-icon>
                  </button>
                  @if (u.status === 'ACTIVE') {
                    <button mat-icon-button type="button" (click)="openDeactivate(u)" aria-label="Desactivar usuario">
                      <mat-icon>person_off</mat-icon>
                    </button>
                  }
                }
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedColumns()"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns()"></tr>
          </table>
        }
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './users.page.scss',
})
export class UsersPage {
  private readonly usersService = inject(UsersService);
  private readonly tenantsService = inject(TenantsService);
  private readonly authStore = inject(AuthStore);
  private readonly tenantStore = inject(TenantStore);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly isPlatformAdmin = computed(() => this.authStore.canManageTenants());
  protected readonly listTenantFilter = signal('');
  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  protected readonly tenants = signal<Tenant[]>([]);

  protected readonly displayedColumns = computed(() =>
    this.isPlatformAdmin()
      ? ['tenantId', 'fullName', 'email', 'roleCode', 'status', 'actions']
      : ['fullName', 'email', 'roleCode', 'status', 'actions'],
  );

  constructor() {
    if (this.isPlatformAdmin()) {
      this.tenantsService
        .list()
        .pipe(catchError(() => of([])))
        .subscribe((tenants) => this.tenants.set(tenants));
    }
  }

  protected pageSubtitle(): string {
    if (this.isPlatformAdmin()) {
      return 'Gestión de usuarios por acueducto (administrador de plataforma).';
    }
    const tenantId = this.tenantStore.tenantId() ?? this.authStore.user()?.tenantId ?? '';
    return `Usuarios del acueducto ${tenantId}.`;
  }

  protected tenantLabel(tenantId: string | null): string {
    if (!tenantId) return '—';
    const tenant = this.tenants().find((t) => t.id === tenantId);
    return tenant ? `${tenant.tenantName} (${tenantId})` : tenantId;
  }

  protected readonly users$ = this.refresh$.pipe(
    switchMap(() => {
      const filter =
        this.isPlatformAdmin() && this.listTenantFilter() ? this.listTenantFilter() : undefined;
      return this.usersService.list(filter).pipe(
        catchError((err) => {
          const msg = err?.error?.message ?? 'No se pudo cargar usuarios';
          this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
          return of([]);
        }),
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected onListFilterChange(tenantId: string) {
    this.listTenantFilter.set(tenantId);
    this.refresh();
  }

  refresh() {
    this.refresh$.next();
  }

  protected canManageRow(user: AppUser): boolean {
    if (this.isPlatformAdmin()) {
      return !!user.tenantId;
    }
    return user.roleCode !== 'PRESIDENT';
  }

  openCreate() {
    const ref = this.dialog.open(UserUpsertDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      data: {
        mode: 'create' as const,
        isPlatformAdmin: this.isPlatformAdmin(),
        tenants: this.tenants(),
      },
    });

    ref.afterClosed().subscribe((res: UserUpsertDialogResult | undefined) => {
      if (!res || res.mode !== 'create') return;
      this.usersService
        .create(res.payload)
        .pipe(
          tap(() => this.snackBar.open('Usuario creado', 'Cerrar', { duration: 3000 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message ?? 'No fue posible crear el usuario';
            this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openUpdate(user: AppUser) {
    const ref = this.dialog.open(UserUpsertDialogComponent, {
      width: '520px',
      maxWidth: '95vw',
      data: {
        mode: 'update' as const,
        user,
        isPlatformAdmin: this.isPlatformAdmin(),
      },
    });

    ref.afterClosed().subscribe((res: UserUpsertDialogResult | undefined) => {
      if (!res || res.mode !== 'update') return;
      this.usersService
        .update(res.id, res.payload)
        .pipe(
          tap(() => this.snackBar.open('Usuario actualizado', 'Cerrar', { duration: 3000 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message ?? 'No fue posible actualizar el usuario';
            this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openDeactivate(user: AppUser) {
    const ref = this.dialog.open(UsersConfirmDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: {
        title: 'Desactivar usuario',
        message: `¿Deseas desactivar a "${user.fullName}"?`,
        confirmText: 'Desactivar',
      },
    });

    ref.afterClosed().subscribe((ok: boolean | undefined) => {
      if (!ok) return;
      this.usersService
        .update(user.id, { status: 'INACTIVE' })
        .pipe(
          tap(() => this.snackBar.open('Usuario desactivado', 'Cerrar', { duration: 3000 })),
          tap(() => this.refresh()),
          catchError((err) => {
            const msg = err?.error?.message ?? 'No fue posible desactivar el usuario';
            this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }

  openResetPassword(user: AppUser) {
    const ref = this.dialog.open(UserResetPasswordDialogComponent, {
      width: '420px',
      maxWidth: '95vw',
      data: { fullName: user.fullName },
    });

    ref.afterClosed().subscribe((password: string | undefined) => {
      if (!password) return;
      this.usersService
        .resetPassword(user.id, password)
        .pipe(
          tap(() => this.snackBar.open('Contraseña actualizada', 'Cerrar', { duration: 3000 })),
          catchError((err) => {
            const msg = err?.error?.message ?? 'No fue posible restablecer la contraseña';
            this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
            return of(null);
          }),
        )
        .subscribe();
    });
  }
}

type UserUpsertDialogData =
  | { mode: 'create'; isPlatformAdmin: boolean; tenants: Tenant[] }
  | { mode: 'update'; user: AppUser; isPlatformAdmin: boolean };

type UserUpsertDialogResult =
  | { mode: 'create'; payload: CreateUserRequest }
  | { mode: 'update'; id: string; payload: UpdateUserRequest };

@Component({
  selector: 'app-user-upsert-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Nuevo usuario' : 'Editar usuario' }}</h2>
    <div mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        @if (data.mode === 'create' && data.isPlatformAdmin) {
          <mat-form-field appearance="outline" class="full">
            <mat-label>Acueducto (tenant)</mat-label>
            <mat-select formControlName="tenantId">
              @for (t of data.tenants; track t.id) {
                <mat-option [value]="t.id">{{ t.tenantName }} ({{ t.id }})</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }
        <mat-form-field appearance="outline" class="full">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="fullName" />
        </mat-form-field>
        @if (data.mode === 'create') {
          <mat-form-field appearance="outline" class="full">
            <mat-label>Correo</mat-label>
            <input matInput type="email" formControlName="email" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="full">
            <mat-label>Contraseña</mat-label>
            <input matInput type="password" formControlName="password" />
          </mat-form-field>
        } @else {
          <mat-form-field appearance="outline" class="full">
            <mat-label>Correo</mat-label>
            <input matInput [value]="data.user.email" disabled />
          </mat-form-field>
        }
        <mat-form-field appearance="outline" class="full">
          <mat-label>Rol</mat-label>
          <mat-select formControlName="roleCode">
            @if (data.isPlatformAdmin) {
              <mat-option value="PRESIDENT">Presidente</mat-option>
            }
            <mat-option value="TREASURY">Tesorería</mat-option>
          </mat-select>
        </mat-form-field>
        @if (data.mode === 'update') {
          <mat-form-field appearance="outline" class="full">
            <mat-label>Estado</mat-label>
            <mat-select formControlName="status">
              <mat-option value="ACTIVE">Activo</mat-option>
              <mat-option value="INACTIVE">Inactivo</mat-option>
            </mat-select>
          </mat-form-field>
        }
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        {{ data.mode === 'create' ? 'Crear' : 'Guardar' }}
      </button>
    </div>
  `,
})
export class UserUpsertDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly data = inject<UserUpsertDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserUpsertDialogComponent>);

  protected readonly form: FormGroup;

  constructor() {
    if (this.data.mode === 'create') {
      this.form = this.data.isPlatformAdmin
        ? this.fb.nonNullable.group({
            tenantId: ['', Validators.required],
            fullName: ['', Validators.required],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(8)]],
            roleCode: ['TREASURY', Validators.required],
          })
        : this.fb.nonNullable.group({
            fullName: ['', Validators.required],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(8)]],
            roleCode: ['TREASURY', Validators.required],
          });
      return;
    }

    const u = this.data.user;
    this.form = this.fb.nonNullable.group({
      fullName: [u.fullName, Validators.required],
      roleCode: [{ value: u.roleCode, disabled: !this.data.isPlatformAdmin && u.roleCode === 'PRESIDENT' }],
      status: [u.status, Validators.required],
    });
  }

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();

    if (this.data.mode === 'create') {
      const payload: CreateUserRequest = {
        fullName: v.fullName.trim(),
        email: v.email.trim(),
        password: v.password as string,
        roleCode: v.roleCode,
      };
      if (this.data.isPlatformAdmin) {
        payload.tenantId = v.tenantId as string;
      }
      this.dialogRef.close({ mode: 'create', payload } satisfies UserUpsertDialogResult);
      return;
    }

    const payload: UpdateUserRequest = {
      fullName: v.fullName.trim(),
      roleCode: v.roleCode,
      status: v.status,
    };
    this.dialogRef.close({
      mode: 'update',
      id: this.data.user.id,
      payload,
    } satisfies UserUpsertDialogResult);
  }
}

@Component({
  selector: 'app-user-reset-password-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Restablecer contraseña</h2>
    <div mat-dialog-content>
      <p>Usuario: <strong>{{ data.fullName }}</strong></p>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Nueva contraseña</mat-label>
          <input matInput type="password" formControlName="password" />
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" [disabled]="form.invalid" (click)="save()">
        Guardar
      </button>
    </div>
  `,
})
export class UserResetPasswordDialogComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly data = inject<{ fullName: string }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserResetPasswordDialogComponent>);

  protected readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  close() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.invalid) return;
    this.dialogRef.close(this.form.controls.password.value);
  }
}

@Component({
  selector: 'app-users-confirm-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <div mat-dialog-content>{{ data.message }}</div>
    <div mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close(false)">Cancelar</button>
      <button mat-flat-button color="warn" type="button" (click)="close(true)">
        {{ data.confirmText ?? 'Confirmar' }}
      </button>
    </div>
  `,
  imports: [MatDialogModule, MatButtonModule],
})
export class UsersConfirmDialogComponent {
  protected readonly data = inject<{ title: string; message: string; confirmText?: string }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UsersConfirmDialogComponent>);

  close(value: boolean) {
    this.dialogRef.close(value);
  }
}

import { AsyncPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, of, shareReplay, switchMap, tap } from 'rxjs';
import { AuthStore } from '../core/auth/auth.store';
import { TenantStore } from '../core/tenant/tenant.store';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { Tenant, TenantsService } from '../tenants/tenants.service';
import { CreateUserRequest, UsersService } from './users.service';

@Component({
  selector: 'app-users-page',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    PageHeaderComponent,
  ],
  template: `
    <app-page-header
      [title]="'Usuarios'"
      [subtitle]="pageSubtitle()"
    >
    </app-page-header>

    <div class="grid">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Nuevo usuario</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="createForm" class="form" (ngSubmit)="createUser()">
            @if (isPlatformAdmin()) {
              <mat-form-field appearance="outline">
                <mat-label>Acueducto (tenant)</mat-label>
                <mat-select formControlName="tenantId">
                  @for (t of tenants(); track t.id) {
                    <mat-option [value]="t.id">{{ t.tenantName }} ({{ t.id }})</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            }
            <mat-form-field appearance="outline">
              <mat-label>Nombre</mat-label>
              <input matInput formControlName="fullName" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Correo</mat-label>
              <input matInput type="email" formControlName="email" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <input matInput type="password" formControlName="password" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Rol</mat-label>
              <mat-select formControlName="roleCode">
                @if (isPlatformAdmin()) {
                  <mat-option value="PRESIDENT">Presidente</mat-option>
                }
                <mat-option value="TREASURY">Tesorería</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" [disabled]="createForm.invalid">Crear</button>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card>
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
            <table mat-table [dataSource]="users" style="width: 100%">
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
              <tr mat-header-row *matHeaderRowDef="columns()"></tr>
              <tr mat-row *matRowDef="let row; columns: columns()"></tr>
            </table>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: `
    .grid {
      display: grid;
      gap: 16px;
    }
    .form {
      display: grid;
      gap: 12px;
      margin-top: 8px;
    }
    .filter {
      width: 100%;
      margin-bottom: 12px;
    }
  `,
})
export class UsersPage {
  private readonly usersService = inject(UsersService);
  private readonly tenantsService = inject(TenantsService);
  private readonly authStore = inject(AuthStore);
  private readonly tenantStore = inject(TenantStore);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isPlatformAdmin = computed(() => this.authStore.canManageTenants());
  protected readonly listTenantFilter = signal('');
  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  protected readonly tenants = signal<Tenant[]>([]);

  protected readonly columns = computed(() =>
    this.isPlatformAdmin()
      ? ['tenantId', 'fullName', 'email', 'roleCode', 'status']
      : ['fullName', 'email', 'roleCode', 'status'],
  );

  protected readonly createForm = this.fb.nonNullable.group({
    tenantId: [''],
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    roleCode: ['TREASURY', Validators.required],
  });

  constructor() {
    if (this.isPlatformAdmin()) {
      this.createForm.controls.tenantId.addValidators(Validators.required);
      this.tenantsService
        .list()
        .pipe(catchError(() => of([])))
        .subscribe((tenants) => this.tenants.set(tenants));
    }
  }

  protected pageSubtitle(): string {
    if (this.isPlatformAdmin()) {
      return 'RF-068: administración de usuarios por acueducto (administrador de plataforma).';
    }
    const tenantId = this.tenantStore.tenantId() ?? this.authStore.user()?.tenantId ?? '';
    return `RF-068: usuarios del acueducto ${tenantId}.`;
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
      return this.usersService.list(filter).pipe(catchError(() => of([])));
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected onListFilterChange(tenantId: string) {
    this.listTenantFilter.set(tenantId);
    this.refresh$.next();
  }

  createUser() {
    if (this.createForm.invalid) return;
    const raw = this.createForm.getRawValue();
    const request: CreateUserRequest = {
      fullName: raw.fullName,
      email: raw.email,
      password: raw.password,
      roleCode: raw.roleCode,
    };
    if (this.isPlatformAdmin()) {
      request.tenantId = raw.tenantId;
    }
    this.usersService
      .create(request)
      .pipe(tap(() => this.refresh$.next()))
      .subscribe({
        next: () => {
          this.createForm.reset({ roleCode: 'TREASURY', tenantId: '' });
          this.snackBar.open('Usuario creado', 'Cerrar', { duration: 3000 });
        },
        error: (err) => {
          const message = err?.error?.message ?? 'No fue posible crear el usuario';
          this.snackBar.open(message, 'Cerrar', { duration: 4000 });
        },
      });
  }
}

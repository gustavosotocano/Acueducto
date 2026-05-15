import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { BehaviorSubject, catchError, of, shareReplay, switchMap, tap } from 'rxjs';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
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
    <app-page-header title="Usuarios" subtitle="RF-066: administración de usuarios del tenant (solo ADMIN).">
    </app-page-header>

    <div class="grid">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Nuevo usuario</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="createForm" class="form" (ngSubmit)="createUser()">
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
                <mat-option value="ADMIN">Administrador</mat-option>
                <mat-option value="PRESIDENT">Presidente</mat-option>
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
          @if (users$ | async; as users) {
            <table mat-table [dataSource]="users" style="width: 100%">
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
              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns"></tr>
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
  `,
})
export class UsersPage {
  private readonly usersService = inject(UsersService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columns = ['fullName', 'email', 'roleCode', 'status'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  protected readonly createForm = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    roleCode: ['TREASURY', Validators.required],
  });

  protected readonly users$ = this.refresh$.pipe(
    switchMap(() => this.usersService.list().pipe(catchError(() => of([])))),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  createUser() {
    if (this.createForm.invalid) return;
    const value = this.createForm.getRawValue() as CreateUserRequest;
    this.usersService
      .create(value)
      .pipe(tap(() => this.refresh$.next()))
      .subscribe({
        next: () => {
          this.createForm.reset({ roleCode: 'TREASURY' });
          this.snackBar.open('Usuario creado', 'Cerrar', { duration: 3000 });
        },
        error: (err) => {
          const message = err?.error?.message ?? 'No fue posible crear el usuario';
          this.snackBar.open(message, 'Cerrar', { duration: 4000 });
        },
      });
  }
}

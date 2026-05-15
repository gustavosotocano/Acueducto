import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-password-reset-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="page">
      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Recuperar contraseña</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (!tokenMode) {
            <form [formGroup]="requestForm" class="form" (ngSubmit)="requestReset()">
              <mat-form-field appearance="outline" class="full">
                <mat-label>Correo registrado</mat-label>
                <input matInput type="email" formControlName="email" />
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit" [disabled]="requestForm.invalid">
                Solicitar token
              </button>
            </form>
          }

          <form [formGroup]="resetForm" class="form" (ngSubmit)="confirmReset()">
            <mat-form-field appearance="outline" class="full">
              <mat-label>Token de recuperación</mat-label>
              <input matInput formControlName="token" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full">
              <mat-label>Nueva contraseña</mat-label>
              <input matInput type="password" formControlName="newPassword" />
            </mat-form-field>
            <button mat-flat-button color="primary" type="submit" [disabled]="resetForm.invalid">
              Actualizar contraseña
            </button>
          </form>

          <p class="hint"><a routerLink="/login">Volver al login</a></p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: `
    .page {
      min-height: 100dvh;
      display: grid;
      place-items: center;
      padding: 24px;
      background: var(--app-bg);
    }
    .card {
      width: min(480px, 100%);
    }
    .form {
      display: grid;
      gap: 12px;
      margin-top: 12px;
    }
    .full {
      width: 100%;
    }
    .hint {
      margin-top: 16px;
      text-align: center;
    }
  `,
})
export class PasswordResetPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected tokenMode = false;

  protected readonly requestForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly resetForm = this.fb.nonNullable.group({
    token: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  requestReset() {
    if (this.requestForm.invalid) return;
    this.authService.requestPasswordReset(this.requestForm.getRawValue().email).subscribe({
      next: (res) => {
        this.tokenMode = true;
        this.snackBar.open(res.message, 'Cerrar', { duration: 6000 });
      },
      error: () => this.snackBar.open('No fue posible solicitar el token', 'Cerrar', { duration: 4000 }),
    });
  }

  confirmReset() {
    if (this.resetForm.invalid) return;
    const { token, newPassword } = this.resetForm.getRawValue();
    this.authService.resetPassword(token, newPassword).subscribe({
      next: (res) => this.snackBar.open(res.message, 'Cerrar', { duration: 5000 }),
      error: (err) => {
        const message = err?.error?.message ?? 'Token inválido o expirado';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }
}

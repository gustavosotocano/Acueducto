import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="page">
      <mat-card class="card">
        <mat-card-header>
          <mat-card-title>Acueducto Veredal</mat-card-title>
          <mat-card-subtitle>Inicie sesión para continuar</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" class="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full">
              <mat-label>Correo</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="username" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full">
              <mat-label>Contraseña</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" />
            </mat-form-field>

            <button mat-flat-button color="primary" type="submit" class="full" [disabled]="form.invalid || saving">
              @if (saving) {
                <mat-spinner diameter="18" />
              } @else {
                Ingresar
              }
            </button>
          </form>

          <p class="hint">
            <a routerLink="/password-reset">¿Olvidó su contraseña?</a>
          </p>
          <p class="dev-hint">Dev: admin@acueducto.local / ChangeMe123!</p>
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
      width: min(420px, 100%);
    }
    .form {
      display: grid;
      gap: 12px;
      margin-top: 8px;
    }
    .full {
      width: 100%;
    }
    .hint {
      margin: 16px 0 0;
      text-align: center;
    }
    .dev-hint {
      margin: 8px 0 0;
      text-align: center;
      font-size: 12px;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected saving = false;

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit() {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    const { email, password } = this.form.getRawValue();
    this.authService.login(email, password).subscribe({
      next: () => {
        this.saving = false;
        void this.router.navigateByUrl('/owners');
      },
      error: (err) => {
        this.saving = false;
        const message = err?.error?.message ?? 'No fue posible iniciar sesión';
        this.snackBar.open(message, 'Cerrar', { duration: 4000 });
      },
    });
  }
}

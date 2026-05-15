import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUser, LoginResponse } from './auth.models';
import { AuthStore } from './auth.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authStore = inject(AuthStore);

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, { email, password })
      .pipe(tap((response) => this.authStore.setSession(response.accessToken, response.user)));
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${environment.apiBaseUrl}/auth/me`).pipe(
      tap((user) => {
        const token = this.authStore.token();
        if (token) {
          this.authStore.setSession(token, user);
        }
      }),
    );
  }

  logout() {
    this.authStore.clear();
  }

  requestPasswordReset(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${environment.apiBaseUrl}/auth/password-reset-request`, {
      email,
    });
  }

  resetPassword(token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${environment.apiBaseUrl}/auth/password-reset`, {
      token,
      newPassword,
    });
  }
}

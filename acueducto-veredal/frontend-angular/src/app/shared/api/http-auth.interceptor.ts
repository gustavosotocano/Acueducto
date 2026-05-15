import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthStore } from '../../core/auth/auth.store';

export const httpAuthInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith(`${environment.apiBaseUrl}/auth/`)) {
    return next(req);
  }

  const token = inject(AuthStore).token();
  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { TenantStore } from '../../core/tenant/tenant.store';

export const httpTenantInterceptor: HttpInterceptorFn = (req, next) => {
  // Do not attach tenant header for tenant administration endpoints.
  if (
    req.url.startsWith(`${environment.apiBaseUrl}/tenants`) ||
    req.url.startsWith(`${environment.apiBaseUrl}/auth/`)
  ) {
    return next(req);
  }

  const tenantId = inject(TenantStore).tenantId()?.trim();
  if (!tenantId) return next(req);

  return next(
    req.clone({
      setHeaders: {
        'X-Tenant-Id': tenantId,
      },
    }),
  );
};


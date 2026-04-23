import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const httpTenantInterceptor: HttpInterceptorFn = (req, next) => {
  const tenantId = environment.tenantId?.trim();
  if (!tenantId) return next(req);

  return next(
    req.clone({
      setHeaders: {
        'X-Tenant-Id': tenantId,
      },
    }),
  );
};


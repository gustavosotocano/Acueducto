import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';
import { UserRole } from './auth.models';

export const roleGuard = (roles: UserRole[]): CanActivateFn => {
  return () => {
    const authStore = inject(AuthStore);
    const router = inject(Router);

    if (authStore.hasRole(...roles)) {
      return true;
    }

    return router.createUrlTree(['/owners']);
  };
};

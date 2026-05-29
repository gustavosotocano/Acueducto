import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login.page').then((m) => m.LoginPage),
    title: 'Login',
  },
  {
    path: 'password-reset',
    loadComponent: () => import('./auth/password-reset/password-reset.page').then((m) => m.PasswordResetPage),
    title: 'Recuperar contraseña',
  },
  {
    path: '',
    loadComponent: () => import('./layout/app-shell.component').then((m) => m.AppShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'tenants',
        loadComponent: () => import('./tenants/tenants.page').then((m) => m.TenantsPage),
        canActivate: [roleGuard(['ADMIN'])],
        title: 'Tenants',
      },
      {
        path: 'users',
        loadComponent: () => import('./users/users.page').then((m) => m.UsersPage),
        canActivate: [roleGuard(['ADMIN', 'PRESIDENT'])],
        title: 'Usuarios',
      },
      {
        path: 'owners',
        loadComponent: () => import('./owners/owners.page').then((m) => m.OwnersPage),
        title: 'Owners',
      },
      {
        path: 'properties',
        loadComponent: () => import('./properties/properties.page').then((m) => m.PropertiesPage),
        title: 'Properties',
      },
      {
        path: 'water-rights',
        loadComponent: () => import('./water-rights/water-rights.page').then((m) => m.WaterRightsPage),
        title: 'Water rights',
      },
      {
        path: 'connection-tariffs',
        loadComponent: () => import('./connection-tariffs/connection-tariffs.page').then((m) => m.ConnectionTariffsPage),
        title: 'Tarifas',
      },
      {
        path: 'invoices',
        loadComponent: () => import('./invoices/invoices-shell.page').then((m) => m.InvoicesShellPage),
        title: 'Invoices',
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'manual' },
          {
            path: 'manual',
            loadComponent: () => import('./invoices/invoices-manual.page').then((m) => m.InvoicesManualPage),
            title: 'Nueva factura',
          },
          {
            path: 'monthly',
            loadComponent: () => import('./invoices/invoices-monthly.page').then((m) => m.InvoicesMonthlyPage),
            title: 'Factura mensual',
          },
        ],
      },
      {
        path: 'payments',
        loadComponent: () => import('./payments/payments.page').then((m) => m.PaymentsPage),
        canActivate: [roleGuard(['ADMIN', 'TREASURY'])],
        title: 'Payments',
      },
      {
        path: 'portfolio',
        loadComponent: () => import('./portfolio/portfolio.page').then((m) => m.PortfolioPage),
        title: 'Portfolio',
      },
      { path: '', pathMatch: 'full', redirectTo: 'owners' },
    ],
  },
  { path: '**', redirectTo: 'owners' },
];

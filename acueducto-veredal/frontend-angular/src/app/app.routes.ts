import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'tenants',
    loadComponent: () => import('./tenants/tenants.page').then((m) => m.TenantsPage),
    title: 'Tenants',
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
    loadComponent: () => import('./invoices/invoices.page').then((m) => m.InvoicesPage),
    title: 'Invoices',
  },
  {
    path: 'payments',
    loadComponent: () => import('./payments/payments.page').then((m) => m.PaymentsPage),
    title: 'Payments',
  },
  {
    path: 'portfolio',
    loadComponent: () => import('./portfolio/portfolio.page').then((m) => m.PortfolioPage),
    title: 'Portfolio',
  },
  { path: '', pathMatch: 'full', redirectTo: 'owners' },
];

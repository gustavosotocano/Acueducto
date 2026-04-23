import { Routes } from '@angular/router';

import { InvoicesPage } from './invoices/invoices.page';
import { OwnersPage } from './owners/owners.page';
import { PaymentsPage } from './payments/payments.page';
import { PortfolioPage } from './portfolio/portfolio.page';
import { WaterRightsPage } from './water-rights/water-rights.page';

export const routes: Routes = [
  { path: 'owners', component: OwnersPage },
  { path: 'water-rights', component: WaterRightsPage },
  { path: 'invoices', component: InvoicesPage },
  { path: 'payments', component: PaymentsPage },
  { path: 'portfolio', component: PortfolioPage },
  { path: '', pathMatch: 'full', redirectTo: 'owners' },
];

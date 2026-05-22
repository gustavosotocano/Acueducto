import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-invoices-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule],
  template: `
    <nav mat-tab-nav-bar [tabPanel]="tabPanel" class="subnav">
      <a
        mat-tab-link
        routerLink="/invoices/manual"
        routerLinkActive
        #manualLink="routerLinkActive"
        [active]="manualLink.isActive"
      >
        Nueva factura
      </a>
      <a
        mat-tab-link
        routerLink="/invoices/monthly"
        routerLinkActive
        #monthlyLink="routerLinkActive"
        [active]="monthlyLink.isActive"
      >
        Factura mensual
      </a>
    </nav>

    <mat-tab-nav-panel #tabPanel>
      <router-outlet />
    </mat-tab-nav-panel>
  `,
  styles: `
    .subnav {
      margin-bottom: 16px;
    }
  `,
})
export class InvoicesShellPage {}

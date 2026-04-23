import { BreakpointObserver } from '@angular/cdk/layout';
import { AsyncPipe } from '@angular/common';
import { Component, ViewChild, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, shareReplay, startWith } from 'rxjs';

type NavItem = {
  label: string;
  icon: string;
  path: string;
};

@Component({
  selector: 'app-shell',
  imports: [
    AsyncPipe,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav
        class="sidenav"
        [mode]="(isDesktop$ | async) ? 'side' : 'over'"
        [opened]="isDesktop$ | async"
      >
        <div class="brand">
          <div class="logo">AV</div>
          <div class="titles">
            <div class="name">Acueducto Veredal</div>
            <div class="subtitle">Operación & cartera</div>
          </div>
        </div>

        <mat-nav-list>
          @for (item of nav; track item.path) {
            <a
              mat-list-item
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: true }"
            >
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar class="toolbar" color="primary">
          @if (!(isDesktop$ | async)) {
            <button mat-icon-button (click)="toggleMobileNav()">
              <mat-icon>menu</mat-icon>
            </button>
          }

          <span class="toolbar-title">{{ currentTitle$ | async }}</span>
          <span class="spacer"></span>

          <button mat-button disabled>
            <mat-icon>apartment</mat-icon>
            <span>Tenant</span>
          </button>
        </mat-toolbar>

        <div class="page">
          <div class="page-inner">
            <router-outlet />
          </div>
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly router = inject(Router);

  protected readonly nav: NavItem[] = [
    { label: 'Owners', icon: 'person', path: '/owners' },
    { label: 'Water rights', icon: 'water_drop', path: '/water-rights' },
    { label: 'Invoices', icon: 'receipt_long', path: '/invoices' },
    { label: 'Payments', icon: 'payments', path: '/payments' },
    { label: 'Portfolio', icon: 'account_balance', path: '/portfolio' },
  ];

  protected readonly isDesktop$ = this.breakpointObserver.observe('(min-width: 1000px)').pipe(
    map((r) => r.matches),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly currentTitle$ = this.router.events.pipe(
    filter((e) => e instanceof NavigationEnd),
    startWith(null),
    map(() => {
      const url = this.router.url.split('?')[0];
      const match = this.nav.find((n) => n.path === url);
      return match?.label ?? 'Acueducto Veredal';
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  @ViewChild(MatSidenav)
  private readonly sidenav?: MatSidenav;

  toggleMobileNav() {
    this.sidenav?.toggle();
  }
}


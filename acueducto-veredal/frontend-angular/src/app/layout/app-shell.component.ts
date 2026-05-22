import { BreakpointObserver } from '@angular/cdk/layout';
import { AsyncPipe } from '@angular/common';
import { Component, ViewChild, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, shareReplay, startWith } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { AuthStore } from '../core/auth/auth.store';
import { TenantStore } from '../core/tenant/tenant.store';
import { TenantsService } from '../tenants/tenants.service';
import { catchError, of } from 'rxjs';

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
    MatMenuModule,
    MatDividerModule,
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
          @for (item of visibleNav(); track item.path) {
            <a
              mat-list-item
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="item.path === '/invoices/manual' ? { exact: false } : { exact: true }"
            >
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar class="toolbar">
          @if (!(isDesktop$ | async)) {
            <button mat-icon-button (click)="toggleMobileNav()">
              <mat-icon>menu</mat-icon>
            </button>
          }

          <span class="toolbar-title">{{ currentTitle$ | async }}</span>
          <span class="spacer"></span>

          @if (authStore.user(); as user) {
            <span class="user-label">{{ user.fullName }} · {{ user.role }}</span>
          }

          @if (canSwitchTenant()) {
            <button mat-button [matMenuTriggerFor]="tenantMenu">
              <mat-icon>apartment</mat-icon>
              <span>{{ activeTenantLabel() }}</span>
            </button>
            <mat-menu #tenantMenu="matMenu">
              <button mat-menu-item [routerLink]="'/tenants'">
                <mat-icon>settings</mat-icon>
                <span>Administrar tenants</span>
              </button>
              <mat-divider />
              @for (t of tenants$ | async; track t.id) {
                <button mat-menu-item (click)="selectTenant(t.id)">
                  <mat-icon>{{ tenantId() === t.id ? 'check' : 'apartment' }}</mat-icon>
                  <span>{{ t.tenantName }}</span>
                </button>
              }
              <mat-divider />
              <button mat-menu-item (click)="clearTenant()">
                <mat-icon>close</mat-icon>
                <span>Limpiar selección</span>
              </button>
            </mat-menu>
          } @else {
            <span class="tenant-readonly">{{ activeTenantLabel() }}</span>
          }

          <button mat-button type="button" (click)="logout()">
            <mat-icon>logout</mat-icon>
            Salir
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
  private readonly tenantStore = inject(TenantStore);
  private readonly tenantsService = inject(TenantsService);
  protected readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);

  private readonly allNav: NavItem[] = [
    { label: 'Tenants', icon: 'apartment', path: '/tenants' },
    { label: 'Usuarios', icon: 'manage_accounts', path: '/users' },
    { label: 'Owners', icon: 'person', path: '/owners' },
    { label: 'Properties', icon: 'home_work', path: '/properties' },
    { label: 'Water rights', icon: 'water_drop', path: '/water-rights' },
    { label: 'Tarifas', icon: 'request_quote', path: '/connection-tariffs' },
    { label: 'Invoices', icon: 'receipt_long', path: '/invoices/manual' },
    { label: 'Payments', icon: 'payments', path: '/payments' },
    { label: 'Portfolio', icon: 'account_balance', path: '/portfolio' },
  ];

  protected visibleNav(): NavItem[] {
    return this.allNav.filter((item) => {
      if (item.path === '/tenants') return this.authStore.canManageTenants();
      if (item.path === '/users') return this.authStore.hasRole('ADMIN');
      if (item.path === '/payments') return this.authStore.canRegisterPayments();
      return true;
    });
  }

  protected canSwitchTenant() {
    return this.authStore.canManageTenants();
  }

  protected readonly isDesktop$ = this.breakpointObserver.observe('(min-width: 1000px)').pipe(
    map((r) => r.matches),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  private readonly routeTitles: { prefix: string; title: string }[] = [
    { prefix: '/invoices/manual', title: 'Nueva factura' },
    { prefix: '/invoices/monthly', title: 'Factura mensual' },
  ];

  protected readonly currentTitle$ = this.router.events.pipe(
    filter((e) => e instanceof NavigationEnd),
    startWith(null),
    map(() => {
      const url = this.router.url.split('?')[0];
      const childTitle = this.routeTitles.find((r) => url === r.prefix || url.startsWith(r.prefix + '/'));
      if (childTitle) return childTitle.title;
      const match = this.allNav.find((n) => n.path === url);
      return match?.label ?? 'Acueducto Veredal';
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly tenants$ = this.tenantsService.list().pipe(
    catchError(() => of([])),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  protected readonly tenantId = this.tenantStore.tenantId;

  protected activeTenantLabel() {
    const id = this.tenantId();
    if (!id) return 'Seleccionar tenant';
    return id;
  }

  protected selectTenant(id: string) {
    this.tenantStore.setTenantId(id);
  }

  protected clearTenant() {
    this.tenantStore.clear();
  }

  @ViewChild(MatSidenav)
  private readonly sidenav?: MatSidenav;

  toggleMobileNav() {
    this.sidenav?.toggle();
  }

  logout() {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }
}


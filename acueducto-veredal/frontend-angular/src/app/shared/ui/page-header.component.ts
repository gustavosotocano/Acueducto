import { Component, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-header',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="page-header">
      <div class="titles">
        <h2 class="title">{{ title }}</h2>
        @if (subtitle) {
          <p class="subtitle">{{ subtitle }}</p>
        }
      </div>

      <div class="actions">
        <ng-content select="[actions]" />
      </div>
    </div>
  `,
  styles: `
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 16px;
    }
    .titles {
      min-width: 0;
    }
    .title {
      margin: 0;
      font: var(--mat-sys-headline-small);
      line-height: 1.2;
    }
    .subtitle {
      margin: 4px 0 0;
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
      line-height: 1.3;
    }
    .actions {
      display: flex;
      gap: 8px;
      align-items: center;
      flex-wrap: wrap;
    }
  `,
})
export class PageHeaderComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle?: string;
}


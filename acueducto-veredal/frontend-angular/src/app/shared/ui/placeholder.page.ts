import { Component, input } from '@angular/core';

@Component({
  selector: 'app-placeholder-page',
  standalone: true,
  template: `
    <h2 style="margin: 0 0 8px">{{ title() }}</h2>
    <p style="margin: 0; color: var(--mat-sys-on-surface-variant)">
      Esta sección está lista para conectar con el backend.
    </p>
  `,
})
export class PlaceholderPage {
  title = input.required<string>();
}


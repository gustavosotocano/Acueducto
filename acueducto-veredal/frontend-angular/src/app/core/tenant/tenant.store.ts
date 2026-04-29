import { Injectable, computed, signal } from '@angular/core';

const STORAGE_KEY = 'acueducto.tenantId';

@Injectable({ providedIn: 'root' })
export class TenantStore {
  private readonly tenantIdSignal = signal<string>(readTenantId());

  readonly tenantId = this.tenantIdSignal.asReadonly();
  readonly hasTenant = computed(() => Boolean(this.tenantIdSignal().trim()));

  setTenantId(id: string) {
    const v = (id ?? '').trim();
    this.tenantIdSignal.set(v);
    if (!v) {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, v);
    }
  }

  clear() {
    this.setTenantId('');
  }
}

function readTenantId(): string {
  try {
    return (localStorage.getItem(STORAGE_KEY) ?? '').trim();
  } catch {
    return '';
  }
}


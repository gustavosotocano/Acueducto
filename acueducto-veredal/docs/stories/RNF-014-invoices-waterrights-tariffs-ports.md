# RNF-014 — Puertos `invoices` ↔ `waterrights` / `tariffs`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/30 |
| **ID** | RNF-014 |
| **Estado** | Completado |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) (patrón establecido) |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** que el módulo `invoices` consulte derechos de agua y tarifas mediante puertos explícitos,  
**para** desacoplar facturación del esquema JPA de `waterrights` y `tariffs` y facilitar cambios en tarifas o batch mensual sin efecto dominó.

---

## Contexto (antes del refactor)

- `InvoiceService` → `WaterRightRepository`, `WaterRightEntity`, `ConnectionTariffRepository`
- `MonthlyInvoiceBatchService` → `WaterRightRepository`, `WaterRightEntity`

---

## Implementación

| Consumidor | Puerto | Adaptador |
|------------|--------|-----------|
| `invoices` | `WaterRightBillingPort` | `waterrights/infrastructure/billing/WaterRightBillingJpaAdapter` |
| `invoices` | `ConnectionTariffLookupPort` | `tariffs/infrastructure/billing/ConnectionTariffLookupJpaAdapter` |

Snapshots: `WaterRightBillingSnapshot`, `ApplicableTariffSnapshot` en `invoices/application/port`.

Servicios refactorizados: `InvoiceService`, `MonthlyInvoiceBatchService`.

---

## Criterios de aceptación

- [x] `invoices/application` no importa `waterrights.infrastructure.*` ni `tariffs.infrastructure.*`
- [x] Puertos cubren validación de derecho + resolución de tarifa aplicable
- [x] `POST /api/v1/invoices` y batch mensual con comportamiento preservado
- [x] Regla documentada en [15-backend-modules-and-api.md](../15-backend-modules-and-api.md)

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- `invoices/application/InvoiceService.java`
- `invoices/application/MonthlyInvoiceBatchService.java`

# RNF-014 — Puertos `invoices` ↔ `waterrights` / `tariffs`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/30 |
| **ID** | RNF-014 |
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

## Contexto

Imports cruzados actuales:

- `InvoiceService` → `WaterRightRepository`, `WaterRightEntity`, `ConnectionTariffRepository`
- `MonthlyInvoiceBatchService` → `WaterRightRepository`, `WaterRightEntity`

La facturación necesita: validar que el derecho existe y pertenece al tenant, obtener `connectionType` para calcular monto, resolver tarifa vigente por fecha.

---

## Alcance

| Consumidor | Puerto sugerido | Proveedor / adaptador |
|------------|-----------------|------------------------|
| `invoices` | `WaterRightBillingPort` | Adaptador en `waterrights/infrastructure` |
| `invoices` | `ConnectionTariffLookupPort` | Adaptador en `tariffs/infrastructure` |

Snapshots mínimos: `WaterRightBillingSnapshot` (id, ownerId, connectionType, status), `ApplicableTariffSnapshot` (amount, validFrom, validTo).

Servicios a refactorizar:

- `InvoiceService`
- `MonthlyInvoiceBatchService`

---

## Criterios de aceptación

- [ ] `invoices/application` no importa `waterrights.infrastructure.*` ni `tariffs.infrastructure.*`
- [ ] Puertos cubren validación de derecho + resolución de tarifa aplicable (misma lógica que hoy)
- [ ] `POST /api/v1/invoices` y `POST /api/v1/invoices/batch/monthly` con comportamiento idéntico
- [ ] Tests unitarios de `InvoiceService` y/o `MonthlyInvoiceBatchService` con fakes de puertos (sin DB)
- [ ] `./scripts/ci.sh backend` en verde
- [ ] Regla documentada o referenciada desde RNF-013

---

## QA — regresión

1. Crear factura manual para un `water_right` activo → monto coherente con tarifa vigente.
2. Batch mensual → mismos skips/creaciones que antes.
3. `water_right` inexistente u otro tenant → error esperado.

---

## Fuera de alcance

- Cambios en endpoints de `waterrights` o `connection-tariffs`
- Refactor de `payments` o `portfolio`

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- `invoices/application/InvoiceService.java`
- `invoices/application/MonthlyInvoiceBatchService.java`

**Agente:** `Dev: toma la RNF-014`

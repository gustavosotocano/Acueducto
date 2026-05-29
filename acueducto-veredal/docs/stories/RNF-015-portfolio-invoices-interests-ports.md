# RNF-015 — Puertos `portfolio` ↔ `invoices` / `interests`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/31 |
| **ID** | RNF-015 |
| **Estado** | Completado |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M–L |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** que el módulo `portfolio` obtenga datos de facturación mediante un puerto de lectura,  
**para** aislar las consultas de cartera del modelo JPA de `invoices` y poder evolucionar reportes sin acoplar entidades ajenas.

---

## Contexto (antes del refactor)

- `PortfolioRepository` referenciaba `InvoiceEntity` y queries sobre tablas de facturas desde `portfolio`.
- `PortfolioService` usaba `InterestPolicyService` en capa application (se mantiene; no es JPA cruzado).

---

## Implementación

| Componente | Ubicación |
|------------|-----------|
| Puerto | `portfolio/application/port/PortfolioReadPort` |
| Snapshots | `WaterRightBalanceSnapshot`, `OwnerBalanceSnapshot`, `OverdueInvoiceSnapshot`, … |
| Adaptador | `invoices/infrastructure/portfolio/PortfolioReadJpaAdapter` |
| Consumidor | `portfolio/application/PortfolioService` (sin `invoices.infrastructure.*`) |

**Decisión:** `InterestPolicyService` directo sigue siendo aceptable; un `InterestPolicyPort` es opcional en el futuro.

---

## Criterios de aceptación

- [x] `portfolio` no importa `invoices.infrastructure.persistence.jpa.*`
- [x] Endpoints `/api/v1/portfolio/*` sin cambio de contrato
- [x] Documentación en [15-backend-modules-and-api.md](../15-backend-modules-and-api.md)

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- `portfolio/application/PortfolioService.java`
- `invoices/infrastructure/portfolio/PortfolioReadJpaAdapter.java`

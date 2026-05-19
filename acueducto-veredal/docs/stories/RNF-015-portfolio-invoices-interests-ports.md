# RNF-015 — Puertos `portfolio` ↔ `invoices` / `interests`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/31 |
| **ID** | RNF-015 |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) (recomendado) |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M–L |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** que el módulo `portfolio` obtenga datos de facturación e intereses mediante puertos de lectura,  
**para** aislar las consultas de cartera del modelo JPA de `invoices` y poder evolucionar reportes sin acoplar entidades ajenas.

---

## Contexto

Acoplamiento actual:

- `PortfolioRepository` extiende `Repository<InvoiceEntity, UUID>` y ejecuta `@Query` sobre tablas de facturas
- `PortfolioService` → `InterestPolicyService` (application directa — aceptable, pero conviene puerto si `interests` crece)

La cartera es **solo lectura**; candidato a `PortfolioReadPort` / `InvoiceOutstandingQueryPort` con proyecciones DTO, no entidades.

---

## Alcance

| Componente | Acción |
|------------|--------|
| `portfolio/application` | Define `PortfolioReadPort` (saldos por water_right, por owner, mora por buckets, filas para cálculo de intereses) |
| `invoices` o `portfolio/infrastructure` | Adaptador que ejecuta las queries hoy en `PortfolioRepository` |
| `portfolio` | `PortfolioRepository` deja de referenciar `InvoiceEntity` como tipo raíz del repositorio |
| `portfolio` | `InterestPolicyPort` (opcional) si se desacopla `InterestPolicyService` directo |

Endpoints sin cambio:

- `GET /api/v1/portfolio/water-rights`
- `GET /api/v1/portfolio/owners`
- `GET /api/v1/portfolio/overdue`
- `GET /api/v1/portfolio/overdue-with-interest`

---

## Criterios de aceptación

- [ ] `portfolio` no importa `invoices.infrastructure.persistence.jpa.*`
- [ ] Resultados numéricos idénticos a los actuales (mismos buckets, totales, intereses)
- [ ] Test unitario de `PortfolioService` con fake de `PortfolioReadPort` para al menos un caso (saldo o mora)
- [ ] `./scripts/ci.sh backend` en verde
- [ ] Sin cambios en contratos REST de `/api/v1/portfolio/*`

---

## QA — regresión

1. Comparar `GET /portfolio/water-rights` y `/owners` antes/después (mismos totales con datos de prueba).
2. `GET /portfolio/overdue` y `/overdue-with-interest` con facturas vencidas e intereses configurados.
3. Validar multitenant: otro `X-Tenant-Id` no mezcla cartera.

---

## Fuera de alcance

- Materialized views / CQRS completo
- Nuevos reportes o export PDF
- Refactor de `invoices` ↔ `waterrights` (RNF-014)

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- `portfolio/infrastructure/persistence/jpa/PortfolioRepository.java`
- `portfolio/application/PortfolioService.java`

**Agente:** `Dev: toma la RNF-015`

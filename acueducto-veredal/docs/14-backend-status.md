# Estado del backend (dónde quedamos)

Documento de referencia para saber qué está implementado hoy en el backend `acueducto-veredal` y qué falta vs `docs/acueducto-veredal-requerimientos.md`.

## Arquitectura modular (puertos)

Los cruces principales entre módulos usan puertos en `application` (sin JPA cruzado). Catálogo completo: [15-backend-modules-and-api.md](15-backend-modules-and-api.md#fronteras-entre-módulos-puertos).

| Cruce | Puerto |
|-------|--------|
| `payments` → `invoices` | `InvoicePaymentPort` |
| `invoices` → `waterrights` / `tariffs` | `WaterRightBillingPort`, `ConnectionTariffLookupPort` |
| `portfolio` → `invoices` | `PortfolioReadPort` |
| `waterrights` → `owners` / `properties` | `OwnerLookupPort`, `PropertyLookupPort` |

**Pendiente (RNF-018):** `properties` → `owners`, `auth` y `shared` → `tenants` aún importan repositorios JPA ajenos. Ver [stories/RNF-018-remaining-module-ports.md](stories/RNF-018-remaining-module-ports.md).

## Implementado

### Multitenancy
- Resolución por header `X-Tenant-Id`.
- Aislamiento por tenant en repositorios (consultas `findByIdAndTenantId`, `findAllByTenantId...`).
- Error “tenant requerido” estandarizado (`TENANT_REQUIRED`).

### Dueños (`owner`)
- Endpoints:
  - `POST /api/v1/owners`
  - `GET /api/v1/owners`
  - `GET /api/v1/owners/{id}`
  - `PUT /api/v1/owners/{id}`
  - `POST /api/v1/owners/{id}/deactivate`
- Soft state `ACTIVE/INACTIVE`.

### Predios (`property`)
- Endpoints:
  - `POST /api/v1/properties`
  - `GET /api/v1/properties`
  - `GET /api/v1/properties/{id}`
  - `PUT /api/v1/properties/{id}`
  - `POST /api/v1/properties/{id}/deactivate`
  - `POST /api/v1/properties/{id}/owner-changes`
  - `GET /api/v1/properties/{id}/owner-history`
- Historial en `property_owner_history`:
  - cierre del registro activo anterior con `endDate = changeDate - 1`
  - nuevo registro activo con `endDate = null`

### Derechos de agua (`water_right`)
- Endpoints:
  - `POST /api/v1/water-rights`
  - `GET /api/v1/water-rights`
  - `GET /api/v1/water-rights/{id}`
  - `PUT /api/v1/water-rights/{id}`
  - `POST /api/v1/water-rights/{id}/deactivate`
  - `POST /api/v1/water-rights/{id}/transfers`
  - `GET /api/v1/water-rights/{id}/owner-history`
- Historial en `water_right_owner_history`:
  - cierre del registro activo anterior con `endDate = transferDate - 1`
  - nuevo registro activo con `endDate = null`
- Deactivate exige `ownershipEndDate`.

### Facturación manual (`invoice`)
- Migración: `V2__invoice.sql`.
- Endpoints:
  - `POST /api/v1/invoices` (creación manual; estado inicial `ISSUED`)
  - `GET /api/v1/invoices` (filtros: `waterRightId`, `ownerId`, `status`, `issueFrom`, `issueTo`)
  - `GET /api/v1/invoices/{id}`

### Historial de estado de factura (RF-054)
- Migración: `V4__invoice_status_history.sql`.
- Endpoints:
  - `POST /api/v1/invoices/{id}/status`
  - `GET /api/v1/invoices/{id}/status-history`

### Pagos (`payment`)
- Migración: `V3__payment.sql`.
- Endpoints:
  - `POST /api/v1/payments` (registra pago a una factura)
  - `GET /api/v1/payments` (filtros: `invoiceId`, `waterRightId`, `ownerId`, `from`, `to`)
- Aplicación de pagos:
  - cuando el total pagado \(\ge\) total de la factura, la factura pasa a `PAID` y queda trazabilidad en historial de estado.

### Cartera / portfolio (saldo pendiente)
- Endpoints:
  - `GET /api/v1/portfolio/water-rights` (pendiente por `water_right`)
  - `GET /api/v1/portfolio/owners` (pendiente consolidado por `owner`)
  - `GET /api/v1/portfolio/overdue` (vencida por buckets 0-30/31-60/61-90/>90, sin intereses)
  - `GET /api/v1/portfolio/overdue-with-interest` (mismo, incluyendo intereses configurables)

### Intereses configurables
- Migración: `V5__interest_config.sql` (`interest_config`, `interest_tier`).
- Endpoints:
  - `GET /api/v1/interests/config`
  - `PUT /api/v1/interests/config`
- Nota:
  - Los tiers (`interest_tier`) se configuran vía BD por ahora (no hay CRUD de tiers todavía).

## Migraciones Flyway actuales
- `V1__init.sql`: esquema base (owners/properties/water_right + historiales + water_meter).
- `V2__invoice.sql`: facturas.
- `V3__payment.sql`: pagos.
- `V4__invoice_status_history.sql`: historial de estado de factura.
- `V5__interest_config.sql`: configuración de intereses.

## Pendientes relevantes (backend)

### Facturación
- **RF-051**: generación periódica/batch de facturas para `water_right` activos.
- Gestión de estados más “operativa”:
  - endpoint para **anular** factura con trazabilidad (y reglas).
  - cálculo o job para estado `OVERDUE` (opcional; hoy se calcula vencimiento en cartera sin cambiar estado).

### Pagos
- **Pago parcial** como estado explícito (p.ej. `PARTIALLY_PAID`) o lectura de “saldo” por factura.
- Reglas de aplicación (p.ej. pago a múltiples facturas, prioridad a vencidas).

### Cartera
- Reportes/exportables (por owner, por periodo, etc.).

### Seguridad
- Autenticación/autorización (el `tenant` hoy depende de header libre).

### Frontend
- `frontend-angular` aún no tiene app Angular (solo `README.md` y `src/assets/i18n`).


# RNF-013 — Fronteras entre módulos del backend (puertos y regla de dependencias)

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/29 |
| **ID** | RNF-013 |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M (piloto) · L (rollout completo) |

---

## Historia de usuario

**Como** desarrollador del backend de Acueducto Veredal,  
**quiero** que los módulos se comuniquen mediante contratos explícitos (puertos) y no mediante entidades/repositorios JPA ajenos,  
**para** reducir acoplamiento, facilitar pruebas unitarias y poder refactorizar cada dominio con un radio de impacto acotado, sin depender de una futura migración a microservicios.

---

## Contexto / problema

Hoy varios servicios de `application` importan directamente clases de `infrastructure.persistence.jpa` de **otros** módulos. Ejemplos:

- `payments` → `InvoiceRepository`, `InvoiceEntity`, `InvoiceStatusService`
- `invoices` → `WaterRightRepository`, `ConnectionTariffRepository`
- `waterrights` → `OwnerRepository`, `PropertyRepository`
- `portfolio` → `InvoiceEntity` en queries de lectura

Esto acelera el MVP, pero:

- Un cambio de esquema JPA en un módulo puede romper la compilación de otros.
- Los tests de un módulo arrastran persistencia ajena o requieren más infraestructura.
- La intención de negocio queda mezclada con detalles de tablas SQL.

Esta historia **no cambia comportamiento funcional** ni contratos REST públicos; mejora la estructura interna del monolito modular.

---

## Objetivo

1. Establecer la **regla de dependencias** entre módulos y documentarla.
2. Introducir **puertos explícitos** en los cruces entre módulos, empezando por un caso piloto.
3. Dejar plantilla y criterios para extender el patrón al resto de cruces.

---

## Alcance

### Fase 1 — Piloto (obligatorio en esta historia)

Refactor del cruce **payments ↔ invoices**:

| Componente | Acción |
|------------|--------|
| `payments/application` | Define `InvoicePaymentPort` (o `InvoiceQueryPort` + operación de marcado pagado) con DTOs/snapshots de aplicación |
| `invoices` | Implementa adaptador en su `infrastructure` que delega a `InvoiceRepository` / `InvoiceStatusService` |
| `PaymentService` | Deja de importar `invoices.infrastructure.*` |
| Tests | Test unitario de `PaymentService` con fake del puerto (sin DB) |
| Tests existentes | `./scripts/ci.sh backend` sigue en verde |

### Fase 2 — Rollout (historias hijas)

| ID | Cruce | Issue |
|----|-------|-------|
| RNF-014 | `invoices` ↔ `waterrights`, `tariffs` | [#30](https://github.com/gustavosotocano/Acueducto/issues/30) |
| RNF-015 | `portfolio` ↔ `invoices`, `interests` | [#31](https://github.com/gustavosotocano/Acueducto/issues/31) |
| RNF-016 | `waterrights` ↔ `owners`, `properties` | [#32](https://github.com/gustavosotocano/Acueducto/issues/32) |

Pendiente futuro: `auth` ↔ `tenants`.

> Recomendación: **una historia por cruce** o por módulo consumidor, para PRs revisables.

---

## Regla de dependencias (definición)

Un módulo `A` **solo puede** importar de un módulo `B`:

| Permitido | No permitido |
|-----------|--------------|
| `B.application` (interfaces / servicios expuestos como puerto) | `B.infrastructure.persistence.jpa.*` |
| `B.domain` (enums, value objects compartidos) | `B.interfaces.rest.*` |
| `shared.*` | Entidades JPA (`*Entity`) de otro módulo |

**Excepción temporal:** adaptadores en `infrastructure` del módulo **proveedor** que implementan puertos definidos por el consumidor o por un paquete `shared.contracts`.

---

## Criterios de aceptación

### Regla y documentación

- [ ] La regla de dependencias está documentada en `docs/15-backend-modules-and-api.md` (sección “Fronteras entre módulos”) o en ADR breve enlazado.
- [ ] El equipo puede verificar el piloto sin imports `*.infrastructure.persistence.jpa` cruzados entre `payments` e `invoices`.

### Piloto payments ↔ invoices

- [ ] Existe interfaz de puerto en capa `application` (lado consumidor o contrato compartido) con métodos mínimos usados por `PaymentService` (consulta factura, validación tenant, aplicar pago / cambio a `PAID` según reglas actuales).
- [ ] Los datos que cruza el puerto son **snapshots/DTOs de aplicación**, no `InvoiceEntity`.
- [ ] La implementación JPA vive solo en el módulo `invoices` (adaptador).
- [ ] Comportamiento funcional idéntico al actual: mismos endpoints, mismos códigos de error, misma lógica de marcar factura `PAID` cuando el total pagado lo exige.
- [ ] Existe al menos un test unitario de `PaymentService` que usa un fake del puerto y **no** levanta base de datos.

### Calidad

- [ ] `./scripts/ci.sh backend` pasa en verde.
- [ ] No se modifican contratos REST (`/api/v1/payments`, `/api/v1/invoices`).
- [ ] Aislamiento multitenant (`tenant_id`) se preserva en el adaptador.

---

## Tareas técnicas sugeridas

1. Inventariar imports cruzados a `infrastructure.jpa` (grep / checklist en PR).
2. Diseñar `InvoicePaymentPort` + records `InvoiceSnapshot`, etc.
3. Implementar `InvoicePaymentJpaAdapter` en `invoices/infrastructure`.
4. Refactorizar `PaymentService` para usar el puerto.
5. Añadir test unitario con fake.
6. Ejecutar suite de integración existente de pagos/facturas.
7. Documentar patrón para siguientes cruces.

---

## Fuera de alcance

- Separar microservicios o bases de datos.
- Introducir cola de mensajes / eventos de dominio (historia futura opcional).
- Refactorizar todos los cruces en un solo PR (solo piloto en RNF-013).
- Cambios en frontend Angular.

---

## Plan de pruebas (dev)

```bash
./scripts/ci.sh backend
```

Casos manuales / integración (si aplica):

1. Registrar pago total sobre factura `ISSUED` → factura pasa a `PAID` + historial de estado.
2. Registrar pago parcial → factura permanece en estado anterior.
3. Pago con `invoiceId` de otro tenant → error de aislamiento (comportamiento actual).

---

## Pasos para QA

**QA scope:** API — regresión de pagos y facturas (sin cambio visible de contrato).

1. Autenticarse y enviar `X-Tenant-Id` válido.
2. Crear factura manual (`POST /api/v1/invoices`).
3. Registrar pago (`POST /api/v1/payments`) con rol `ADMIN` o `TREASURY`.
4. Verificar estado de factura (`GET /api/v1/invoices/{id}`) e historial (`GET /api/v1/invoices/{id}/status-history`).
5. Repetir pago parcial y validar que el estado no cambia a `PAID` hasta cubrir el total.

---

## Definición de terminado (DoD)

- [ ] PR mergeado con piloto `payments` ↔ `invoices` completo.
- [ ] Criterios de aceptación marcados.
- [ ] `./scripts/ci.sh backend` verificado en PR.
- [ ] Documentación actualizada.
- [ ] Sin imports prohibidos en el cruce piloto.

---

## Valor para el negocio (por qué priorizarla)

- Menos riesgo al evolucionar facturación y pagos (módulos críticos).
- Entregas más rápidas cuando varios desarrolladores tocan módulos distintos.
- Base para reportes/cartera más complejos sin “efecto dominó” en compilación.

---

## Referencias

- [ADR-001 — Monolito modular multitenant](../11-adr-001-monolith-modular.md)
- [Módulos y API REST](../15-backend-modules-and-api.md)
- [Estado del backend](../14-backend-status.md)
- Código piloto: `payments/application/PaymentService.java`, `invoices/application/InvoiceStatusService.java`

---

## Frase para el agente / board

`Dev: toma la RNF-013`

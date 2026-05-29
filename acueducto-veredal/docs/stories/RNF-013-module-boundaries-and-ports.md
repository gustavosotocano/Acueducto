# RNF-013 — Fronteras entre módulos del backend (puertos y regla de dependencias)

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/29 |
| **ID** | RNF-013 |
| **Estado** | Completado |
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

## Contexto / problema (antes del refactor)

Varios servicios de `application` importaban directamente clases de `infrastructure.persistence.jpa` de **otros** módulos. Ejemplos:

- `payments` → `InvoiceRepository`, `InvoiceEntity`, `InvoiceStatusService`
- `invoices` → `WaterRightRepository`, `ConnectionTariffRepository`
- `waterrights` → `OwnerRepository`, `PropertyRepository`
- `portfolio` → `InvoiceEntity` en queries de lectura

Esto aceleraba el MVP, pero un cambio de esquema JPA en un módulo podía romper la compilación de otros.

**Rollout:** los cruces anteriores se cerraron en RNF-014, RNF-015 y RNF-016. Excepciones restantes: [RNF-018](RNF-018-remaining-module-ports.md).

---

## Objetivo

1. Establecer la **regla de dependencias** entre módulos y documentarla.
2. Introducir **puertos explícitos** en los cruces entre módulos, empezando por un caso piloto.
3. Dejar plantilla y criterios para extender el patrón al resto de cruces.

---

## Implementación (piloto completado)

| Componente | Ubicación |
|------------|-----------|
| Puerto | `payments/application/port/InvoicePaymentPort` |
| Snapshot | `payments/application/port/InvoiceForPayment` |
| Adaptador | `invoices/infrastructure/payment/InvoicePaymentJpaAdapter` |
| Consumidor | `payments/application/PaymentService` (sin `invoices.infrastructure.*`) |

---

## Regla de dependencias (definición vigente)

Un módulo `A` **solo puede** importar de un módulo `B`:

| Permitido | No permitido |
|-----------|--------------|
| `B.application` (puertos, snapshots) | `B.infrastructure.persistence.jpa.*` |
| `B.domain` (enums, value objects compartidos) | `B.interfaces.rest.*` |
| `shared.*` | Entidades JPA (`*Entity`) de otro módulo |

Los adaptadores JPA implementan el puerto en el módulo **proveedor** de datos.

Documentación de referencia: [15-backend-modules-and-api.md](../15-backend-modules-and-api.md#fronteras-entre-módulos-puertos).

---

## Criterios de aceptación

### Regla y documentación

- [x] La regla de dependencias está documentada en `docs/15-backend-modules-and-api.md`.
- [x] Sin imports `*.infrastructure.persistence.jpa` cruzados entre `payments` e `invoices`.

### Piloto payments ↔ invoices

- [x] `InvoicePaymentPort` + `InvoiceForPayment` en capa `application`.
- [x] Implementación JPA solo en `invoices` (`InvoicePaymentJpaAdapter`).
- [x] Comportamiento funcional preservado (pagos, estado `PAID`, multitenant).

### Rollout (historias hijas)

| ID | Cruce | Estado |
|----|-------|--------|
| RNF-014 | `invoices` ↔ `waterrights`, `tariffs` | Completado |
| RNF-015 | `portfolio` ↔ `invoices` | Completado |
| RNF-016 | `waterrights` ↔ `owners`, `properties` | Completado |
| RNF-018 | `properties`, `auth`, `shared` ↔ resto | Pendiente |

---

## Referencias

- [ADR-001 — Monolito modular multitenant](../11-adr-001-monolith-modular.md)
- [Módulos y API REST](../15-backend-modules-and-api.md)
- [Estado del backend](../14-backend-status.md)

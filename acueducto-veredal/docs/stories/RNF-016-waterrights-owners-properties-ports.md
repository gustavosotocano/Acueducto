# RNF-016 — Puertos `waterrights` ↔ `owners` / `properties`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/32 |
| **ID** | RNF-016 |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) (patrón establecido) |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** que `waterrights` valide owners y predios mediante puertos,  
**para** mantener el núcleo del dominio (`water_right`) independiente del mapeo JPA de `owners` y `properties` al crear, actualizar o transferir derechos.

---

## Contexto

Imports cruzados actuales:

- `WaterRightService` → `OwnerRepository`, `PropertyRepository`
- `WaterRightTransferService` → `OwnerRepository`, `OwnerEntity`

También relacionado (mismo epic, otro módulo):

- `PropertyService` / `PropertyOwnershipService` → `OwnerRepository` — **fuera de alcance** salvo que se incluya sub-tarea explícita; esta historia se centra en `waterrights`.

---

## Alcance

| Consumidor | Puerto sugerido | Proveedor |
|------------|-----------------|-----------|
| `waterrights` | `OwnerLookupPort` | Adaptador en `owners/infrastructure` |
| `waterrights` | `PropertyLookupPort` | Adaptador en `properties/infrastructure` |

Snapshots: `OwnerSnapshot` (id, status), `PropertySnapshot` (id, status, ownerId).

Servicios a refactorizar:

- `WaterRightService`
- `WaterRightTransferService`

---

## Criterios de aceptación

- [ ] `waterrights/application` no importa `owners.infrastructure.*` ni `properties.infrastructure.*`
- [ ] Crear/actualizar/desactivar/transferir `water_right` con mismas validaciones y errores
- [ ] Historial de propietarios (`owner-history`) sin cambios funcionales
- [ ] Test unitario de `WaterRightTransferService` (o `WaterRightService`) con fakes de puertos
- [ ] `./scripts/ci.sh backend` en verde
- [ ] Endpoints `/api/v1/water-rights/*` sin cambio de contrato

---

## QA — regresión

1. `POST /water-rights` con owner y property válidos del tenant.
2. `POST /water-rights/{id}/transfers` → historial actualizado, id del derecho conservado.
3. Owner/property inactivo o de otro tenant → rechazo esperado.

---

## Fuera de alcance

- Refactor `properties` → `owners` (`PropertyOwnershipService`)
- Cambios en APIs de `owners` o `properties`

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- `waterrights/application/WaterRightService.java`
- `waterrights/application/transfer/WaterRightTransferService.java`

**Agente:** `Dev: toma la RNF-016`

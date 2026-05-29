# RNF-016 — Puertos `waterrights` ↔ `owners` / `properties`

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/32 |
| **ID** | RNF-016 |
| **Estado** | Completado |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) |
| **Prioridad** | Media |
| **QA scope** | API |
| **Estimación sugerida** | M |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** que `waterrights` valide owners y predios mediante puertos,  
**para** mantener el núcleo del dominio (`water_right`) independiente del mapeo JPA de `owners` y `properties` al crear, actualizar o transferir derechos.

---

## Contexto (antes del refactor)

- `WaterRightService` → `OwnerRepository`, `PropertyRepository`
- `WaterRightTransferService` → `OwnerRepository`, `OwnerEntity`

**Fuera de alcance de esta historia (RNF-018):** `PropertyService` / `PropertyOwnershipService` → `OwnerRepository`.

---

## Implementación

| Consumidor | Puerto | Adaptador |
|------------|--------|-----------|
| `waterrights` | `OwnerLookupPort` | `owners/infrastructure/lookup/OwnerLookupJpaAdapter` |
| `waterrights` | `PropertyLookupPort` | `properties/infrastructure/lookup/PropertyLookupJpaAdapter` |

Snapshots: `OwnerSnapshot`, `PropertySnapshot` en `waterrights/application/port`.

Servicios refactorizados: `WaterRightService`, `WaterRightTransferService`.

---

## Criterios de aceptación

- [x] `waterrights/application` no importa `owners.infrastructure.*` ni `properties.infrastructure.*`
- [x] Crear/actualizar/desactivar/transferir `water_right` con mismas validaciones
- [x] Endpoints `/api/v1/water-rights/*` sin cambio de contrato
- [x] Documentación en [15-backend-modules-and-api.md](../15-backend-modules-and-api.md)

---

## Referencias

- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- [RNF-018](RNF-018-remaining-module-ports.md) — `properties` → `owners`
- `waterrights/application/WaterRightService.java`
- `waterrights/application/transfer/WaterRightTransferService.java`

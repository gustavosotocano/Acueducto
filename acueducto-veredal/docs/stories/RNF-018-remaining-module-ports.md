# RNF-018 — Puertos restantes (`properties`, `auth`, `shared` ↔ `tenants` / `owners`)

| Campo | Valor |
|-------|-------|
| **ID** | RNF-018 |
| **Tipo** | Historia técnica / habilitador de arquitectura |
| **Épica** | Arquitectura backend — calidad y mantenibilidad |
| **Depende de** | [RNF-013](RNF-013-module-boundaries-and-ports.md) · [RNF-016](RNF-016-waterrights-owners-properties-ports.md) |
| **Prioridad** | Baja–Media |
| **QA scope** | API |
| **Estimación sugerida** | M |

---

## Historia de usuario

**Como** desarrollador del backend,  
**quiero** cerrar los últimos cruces que aún importan JPA de otros módulos,  
**para** que la regla de fronteras por puertos se cumpla en todo el monolito sin excepciones documentadas.

---

## Contexto

Tras RNF-013 a RNF-016, los cruces comerciales y `waterrights` ya usan puertos. Quedan imports cruzados a `infrastructure.persistence.jpa` ajeno:

| Consumidor | Archivo(s) | Importa de |
|------------|------------|------------|
| `properties` | `PropertyService`, `PropertyOwnershipService` | `owners` (`OwnerRepository`, `OwnerEntity`) |
| `auth` | `UserService` | `tenants` (`TenantRepository`) |
| `shared` | `TenantExistsFilter` | `tenants` (`TenantRepository`) |

Esta historia **no cambia contratos REST** ni esquema Flyway; solo estructura interna.

---

## Alcance

| Consumidor | Puerto sugerido | Proveedor / adaptador |
|------------|-----------------|------------------------|
| `properties` | Reutilizar `OwnerLookupPort` (definido en `waterrights/application/port`) o `OwnerValidationPort` en `properties/application/port` | Adaptador en `owners/infrastructure/lookup` (ampliar el existente si hace falta) |
| `auth` | `TenantLookupPort` | Adaptador en `tenants/infrastructure/lookup` |
| `shared` | Mismo `TenantLookupPort` (inyectado en filtro) | Mismo adaptador |

Snapshots mínimos: `OwnerSnapshot` (ya existe), `TenantSnapshot` (id, status o flag `exists`).

Servicios a refactorizar:

- `PropertyService`
- `PropertyOwnershipService`
- `UserService`
- `TenantExistsFilter`

---

## Criterios de aceptación

- [ ] `properties/application` no importa `owners.infrastructure.persistence.jpa.*`
- [ ] `auth/application` no importa `tenants.infrastructure.persistence.jpa.*`
- [ ] `shared/multitenancy` no importa `tenants.infrastructure.persistence.jpa.*`
- [ ] Crear/actualizar predio, cambio de propietario, alta de usuario y validación de tenant en filtro con mismo comportamiento y errores
- [ ] `./scripts/ci.sh backend` en verde
- [ ] Documentación en [15-backend-modules-and-api.md](../15-backend-modules-and-api.md): tabla “Excepciones conocidas” vacía o eliminada

---

## QA — regresión

1. `POST /api/v1/properties` con owner válido del tenant → éxito; owner inexistente → error esperado.
2. `POST /api/v1/properties/{id}/owner-changes` → historial coherente.
3. `POST /api/v1/users` con tenant válido → éxito.
4. Request con `X-Tenant-Id` inexistente → rechazo del filtro (comportamiento actual).

---

## Fuera de alcance

- Nuevos endpoints o roles
- Extraer `InterestPolicyPort` (`portfolio` ↔ `interests`) — opcional en historia futura
- Microservicios o eventos de dominio

---

## Referencias

- [15-backend-modules-and-api.md](../15-backend-modules-and-api.md)
- [RNF-013](RNF-013-module-boundaries-and-ports.md)
- [RNF-016](RNF-016-waterrights-owners-properties-ports.md)

**Agente:** `Dev: toma la RNF-018`

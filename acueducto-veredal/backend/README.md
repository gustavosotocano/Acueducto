# Acueducto Veredal Backend

Base inicial en Spring Boot para un sistema de acueducto veredal multi-tenant.

## Decisiones incluidas

- `tenant_id` en tablas core
- Core de negocio centrado en `water_right`
- Historial de transferencia de derecho de agua en `water_right_owner_history`
- Historial de propiedad del predio en `property_owner_history`
- Flyway inicial con tablas y constraints base
- Controllers iniciales para `owners` y `water_rights`
- `X-Tenant-Id` como header inicial para resolver tenant

## Capa agregada en esta iteración

Se agregó la primera capa transaccional real alrededor del negocio principal:

- caso de uso de transferencia de `water_right`
- endpoint para transferir el derecho sin cambiar su `id`
- cierre del historial activo anterior
- apertura de nuevo historial en `water_right_owner_history`
- registro automático del historial inicial cuando se crea el `water_right`
- endpoint para consultar el historial de propietarios del derecho

## Endpoints nuevos

### Transferir derecho de agua

```http
POST /api/v1/water-rights/{id}/transfers
X-Tenant-Id: demo
Content-Type: application/json
```

Body:

```json
{
  "newOwnerId": "11111111-1111-1111-1111-111111111111",
  "transferDate": "2026-03-18",
  "transferReason": "SALE",
  "notes": "Transferencia por compraventa del derecho"
}
```

### Consultar historial de propietarios

```http
GET /api/v1/water-rights/{id}/owner-history
X-Tenant-Id: demo
```

## Cómo correr

```bash
mvn spring-boot:run
```

## Header requerido

```text
X-Tenant-Id: demo
```

## Próximos pasos recomendados

1. Completar entidades `PropertyEntity`, `WaterMeterEntity` y sus endpoints.
2. Implementar update/deactivate para `owner` y `water_right`.
3. Crear caso de uso de cambio de propiedad (`property_owner_history`).
4. Agregar enforcement fuerte de tenant en capa repositorio/hibernate filter.
5. Incorporar tests con Testcontainers y fixtures Flyway.

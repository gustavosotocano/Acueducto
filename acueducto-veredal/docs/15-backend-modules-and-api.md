# Backend — Módulos y API REST

Referencia de la arquitectura modular del backend y catálogo de endpoints. Complementa [ADR-001](11-adr-001-monolith-modular.md) y [estado del backend](14-backend-status.md).

## Resumen arquitectónico

| Aspecto | Decisión |
|---------|----------|
| Estilo | Monolito modular multitenant |
| Patrón | Arquitectura hexagonal (por módulo) |
| Stack | Spring Boot 3.3 · Java 21 · PostgreSQL · Flyway · JWT |
| Concurrencia | Virtual threads (`spring.threads.virtual.enabled`) |
| Contexto tenant | `ScopedValue` en `TenantContext` (preview en Java 21; `--enable-preview` en Maven) |
| Aislamiento | `tenant_id` + header `X-Tenant-Id` |
| Entidad central | `water_right` (derecho de agua) |

---

## Diagrama de módulos

```mermaid
flowchart TB
    subgraph Monolito["acueducto-veredal (monolito modular multitenant)"]
        direction TB

        subgraph Plataforma["Plataforma / cross-cutting"]
            shared["shared<br/>multitenancy · security · config · API errors"]
            tenants["tenants<br/>acueductos veredales"]
            auth["auth<br/>usuarios · JWT · login"]
        end

        subgraph Core["Núcleo del negocio"]
            owners["owners<br/>dueños de derecho"]
            properties["properties<br/>predios + historial propiedad"]
            waterrights["waterrights ★<br/>derecho de agua + transferencias"]
        end

        subgraph Comercial["Facturación y cobro"]
            tariffs["tariffs<br/>tarifas por tipo conexión"]
            invoices["invoices<br/>facturas + batch mensual"]
            payments["payments<br/>pagos / recibos"]
            interests["interests<br/>política de intereses"]
            portfolio["portfolio<br/>cartera y mora"]
        end
    end

    DB[(PostgreSQL<br/>Flyway + JPA)]

    shared --> tenants
    shared --> auth
    auth --> tenants

    owners --> shared
    properties --> owners
    properties --> shared
    waterrights --> owners
    waterrights --> properties
    waterrights --> shared

    tariffs --> waterrights
    tariffs --> shared
    invoices --> waterrights
    invoices --> tariffs
    invoices --> shared
    payments --> invoices
    payments --> shared
    interests --> shared
    portfolio --> invoices
    portfolio --> interests
    portfolio --> shared

    tenants --> DB
    auth --> DB
    owners --> DB
    properties --> DB
    waterrights --> DB
    tariffs --> DB
    invoices --> DB
    payments --> DB
    interests --> DB
    portfolio --> DB
```

### Dependencias entre módulos

| Módulo | Rol | Depende de |
|--------|-----|------------|
| **shared** | Multitenancy, seguridad, errores globales | — |
| **tenants** | Registro de acueductos (`tenant_id`) | `shared` |
| **auth** | Usuarios, roles, JWT | `tenants`, `shared` |
| **owners** | Personas dueñas de derechos | `shared` |
| **properties** | Predios e historial de propiedad | `owners` |
| **waterrights** | Derecho de agua, transferencias, historial | `owners`, `properties` |
| **tariffs** | Tarifas por `ConnectionType` | `waterrights.domain` |
| **invoices** | Facturación manual y batch mensual | `waterrights`, `tariffs` |
| **payments** | Pagos; marca factura `PAID` | `invoices` |
| **interests** | Configuración de intereses por mora | `shared` |
| **portfolio** | Saldos, vencidos, intereses | `invoices`, `interests` |

### Flujo principal del dominio

```mermaid
flowchart LR
    T[tenants] --> O[owners]
    O --> P[properties]
    O --> WR["waterrights ★"]
    P --> WR
    WR --> TR[tariffs]
    TR --> INV[invoices]
    INV --> PAY[payments]
    INV --> PF[portfolio]
    INT[interests] --> PF
```

### Capas internas (hexagonal por módulo)

```mermaid
flowchart LR
    HTTP["Cliente HTTP<br/>(Angular)"] --> IF["interfaces/rest<br/>Controllers + DTOs"]
    IF --> APP["application<br/>Services / casos de uso"]
    APP --> INF["infrastructure/persistence/jpa<br/>Entities + Repositories"]
    INF --> DB[(PostgreSQL)]

    DOM["domain<br/>enums / reglas"] -.-> APP
    DOM -.-> INF

    shared_ctx["shared.TenantContext"] -.-> APP
```

### Notas de diseño

- Un solo JAR desplegable; separación por paquetes Java (`com.acueducto.veredal.<módulo>`).
- Los módulos se acoplan de forma pragmática vía repositorios/entidades JPA del otro módulo (hexagonal liviano para el MVP).
- `waterrights` es el hub del modelo: facturación, cartera y tarifas giran alrededor de él.

---

## Convenciones de la API

### Headers

| Header | Requerido | Descripción |
|--------|-----------|-------------|
| `X-Tenant-Id` | Sí (casi todos los endpoints de negocio) | Identificador del acueducto veredal |
| `Authorization` | Sí (excepto login y reset de contraseña) | `Bearer <JWT>` |

### Autenticación

Endpoints **públicos** (sin JWT):

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/password-reset-request`
- `POST /api/v1/auth/password-reset`

Todos los demás requieren JWT. Algunos endpoints además exigen rol:

| Rol | Endpoints restringidos |
|-----|------------------------|
| `ADMIN` | `POST/PUT/POST deactivate` en tenants; todo `/api/v1/users`; `POST /api/v1/payments` |
| `ADMIN` o `TREASURY` | `POST /api/v1/payments` |

---

## Diagrama de endpoints por módulo

```mermaid
flowchart TB
    subgraph API["/api/v1"]
        direction TB

        subgraph AuthMod["auth"]
            A1["POST /auth/login"]
            A2["GET /auth/me"]
            A3["POST /auth/password-reset-request"]
            A4["POST /auth/password-reset"]
        end

        subgraph UsersMod["auth · users"]
            U1["POST /users"]
            U2["GET /users"]
            U3["GET /users/{id}"]
            U4["PUT /users/{id}"]
            U5["POST /users/{id}/reset-password"]
        end

        subgraph TenantsMod["tenants"]
            T1["POST /tenants"]
            T2["GET /tenants"]
            T3["GET /tenants/{id}"]
            T4["PUT /tenants/{id}"]
            T5["POST /tenants/{id}/deactivate"]
        end

        subgraph CoreMod["núcleo"]
            O1["owners CRUD + deactivate"]
            P1["properties CRUD + owner-changes + owner-history"]
            W1["water-rights CRUD + transfers + owner-history"]
        end

        subgraph BillingMod["facturación"]
            CT1["connection-tariffs CRUD + applicable"]
            I1["invoices CRUD + batch/monthly + status"]
            PAY1["payments POST + GET"]
        end

        subgraph PortfolioMod["cartera"]
            PF1["portfolio water-rights · owners · overdue"]
            IC1["interests/config GET + PUT"]
        end
    end

    Client["Angular / cliente HTTP"] --> API
```

### Mapa REST completo (árbol)

```mermaid
flowchart LR
    root["/api/v1"]

    root --> auth
    root --> users
    root --> tenants
    root --> owners
    root --> properties
    root --> wr["water-rights"]
    root --> ct["connection-tariffs"]
    root --> invoices
    root --> payments
    root --> portfolio
    root --> interests

    auth --> a_login["POST /login"]
    auth --> a_me["GET /me"]
    auth --> a_pr["POST /password-reset-request"]
    auth --> a_p["POST /password-reset"]

    wr --> wr_t["POST /{id}/transfers"]
    wr --> wr_h["GET /{id}/owner-history"]

    properties --> p_oc["POST /{id}/owner-changes"]
    properties --> p_oh["GET /{id}/owner-history"]

    ct --> ct_app["GET /applicable"]

    invoices --> inv_b["POST /batch/monthly"]
    invoices --> inv_s["POST /{id}/status"]
    invoices --> inv_sh["GET /{id}/status-history"]

    portfolio --> pf_wr["GET /water-rights"]
    portfolio --> pf_o["GET /owners"]
    portfolio --> pf_od["GET /overdue"]
    portfolio --> pf_odi["GET /overdue-with-interest"]
```

---

## Catálogo de endpoints

Leyenda: **Auth** = JWT requerido · **Rol** = `@PreAuthorize` adicional · **Tenant** = header `X-Tenant-Id` requerido.

### auth — `/api/v1/auth`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/login` | No | — | No | Login; devuelve `accessToken` |
| `GET` | `/me` | Sí | — | No* | Perfil del usuario autenticado |
| `POST` | `/password-reset-request` | No | — | No | Solicitar token de reset |
| `POST` | `/password-reset` | No | — | No | Confirmar reset con token |

\* El tenant del usuario viene en el JWT; no requiere `X-Tenant-Id` explícito.

### auth · users — `/api/v1/users`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/` | Sí | `ADMIN` | Sí | Crear usuario |
| `GET` | `/` | Sí | `ADMIN` | Sí | Listar usuarios del tenant |
| `GET` | `/{id}` | Sí | `ADMIN` | Sí | Obtener usuario |
| `PUT` | `/{id}` | Sí | `ADMIN` | Sí | Actualizar usuario |
| `POST` | `/{id}/reset-password` | Sí | `ADMIN` | Sí | Reset de contraseña por admin |

### tenants — `/api/v1/tenants`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/` | Sí | `ADMIN` | No | Crear acueducto (tenant) |
| `GET` | `/` | Sí | — | No | Listar tenants |
| `GET` | `/{id}` | Sí | — | No | Obtener tenant |
| `PUT` | `/{id}` | Sí | `ADMIN` | No | Actualizar tenant |
| `POST` | `/{id}/deactivate` | Sí | `ADMIN` | No | Desactivar tenant |

### owners — `/api/v1/owners`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/` | Sí | — | Sí | Crear dueño |
| `GET` | `/` | Sí | — | Sí | Listar dueños |
| `GET` | `/{id}` | Sí | — | Sí | Obtener dueño |
| `PUT` | `/{id}` | Sí | — | Sí | Actualizar dueño |
| `POST` | `/{id}/deactivate` | Sí | — | Sí | Desactivar dueño (`ACTIVE` → `INACTIVE`) |

### properties — `/api/v1/properties`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/` | Sí | — | Sí | Crear predio |
| `GET` | `/` | Sí | — | Sí | Listar predios |
| `GET` | `/{id}` | Sí | — | Sí | Obtener predio |
| `PUT` | `/{id}` | Sí | — | Sí | Actualizar predio |
| `POST` | `/{id}/deactivate` | Sí | — | Sí | Desactivar predio |
| `POST` | `/{id}/owner-changes` | Sí | — | Sí | Cambio de propietario del predio |
| `GET` | `/{id}/owner-history` | Sí | — | Sí | Historial de propietarios del predio |

### waterrights — `/api/v1/water-rights`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `POST` | `/` | Sí | — | Sí | Crear derecho de agua |
| `GET` | `/` | Sí | — | Sí | Listar derechos |
| `GET` | `/{id}` | Sí | — | Sí | Obtener derecho |
| `PUT` | `/{id}` | Sí | — | Sí | Actualizar derecho |
| `POST` | `/{id}/deactivate` | Sí | — | Sí | Desactivar derecho |
| `POST` | `/{id}/transfers` | Sí | — | Sí | Transferir derecho (mantiene `id`) |
| `GET` | `/{id}/owner-history` | Sí | — | Sí | Historial de propietarios del derecho |

### tariffs — `/api/v1/connection-tariffs`

| Método | Ruta | Auth | Rol | Tenant | Query params | Descripción |
|--------|------|------|-----|--------|--------------|-------------|
| `POST` | `/` | Sí | — | Sí | — | Crear tarifa |
| `GET` | `/` | Sí | — | Sí | — | Listar tarifas |
| `GET` | `/applicable` | Sí | — | Sí | `connectionType`, `onDate` | Tarifa vigente para una fecha |
| `GET` | `/{id}` | Sí | — | Sí | — | Obtener tarifa |
| `PUT` | `/{id}` | Sí | — | Sí | — | Actualizar tarifa |

### invoices — `/api/v1/invoices`

| Método | Ruta | Auth | Rol | Tenant | Query params | Descripción |
|--------|------|------|-----|--------|--------------|-------------|
| `POST` | `/` | Sí | — | Sí | — | Crear factura manual (`ISSUED`) |
| `POST` | `/batch/monthly` | Sí | — | Sí | — | Generación masiva mensual |
| `GET` | `/` | Sí | — | Sí | `waterRightId`, `ownerId`, `status`, `issueFrom`, `issueTo` | Listar facturas |
| `GET` | `/{id}` | Sí | — | Sí | — | Obtener factura |
| `POST` | `/{id}/status` | Sí | — | Sí | — | Cambiar estado de factura |
| `GET` | `/{id}/status-history` | Sí | — | Sí | — | Historial de estados |

### payments — `/api/v1/payments`

| Método | Ruta | Auth | Rol | Tenant | Query params | Descripción |
|--------|------|------|-----|--------|--------------|-------------|
| `POST` | `/` | Sí | `ADMIN`, `TREASURY` | Sí | — | Registrar pago (puede marcar factura `PAID`) |
| `GET` | `/` | Sí | — | Sí | `invoiceId`, `waterRightId`, `ownerId`, `from`, `to` | Listar pagos |

### portfolio — `/api/v1/portfolio`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `GET` | `/water-rights` | Sí | — | Sí | Saldo pendiente por derecho de agua |
| `GET` | `/owners` | Sí | — | Sí | Saldo pendiente consolidado por dueño |
| `GET` | `/overdue` | Sí | — | Sí | Mora por buckets (0-30 / 31-60 / 61-90 / >90), sin intereses |
| `GET` | `/overdue-with-interest` | Sí | — | Sí | Mora con intereses según configuración |

### interests — `/api/v1/interests/config`

| Método | Ruta | Auth | Rol | Tenant | Descripción |
|--------|------|------|-----|--------|-------------|
| `GET` | `/` | Sí | — | Sí | Obtener configuración de intereses |
| `PUT` | `/` | Sí | — | Sí | Actualizar configuración de intereses |

---

## Resumen por módulo (conteo)

| Módulo | Base path | Endpoints |
|--------|-----------|-----------|
| auth | `/api/v1/auth` | 4 |
| users | `/api/v1/users` | 5 |
| tenants | `/api/v1/tenants` | 5 |
| owners | `/api/v1/owners` | 5 |
| properties | `/api/v1/properties` | 7 |
| waterrights | `/api/v1/water-rights` | 7 |
| tariffs | `/api/v1/connection-tariffs` | 5 |
| invoices | `/api/v1/invoices` | 6 |
| payments | `/api/v1/payments` | 2 |
| portfolio | `/api/v1/portfolio` | 4 |
| interests | `/api/v1/interests/config` | 2 |
| **Total** | | **52** |

---

## Ejemplo de request

```http
GET /api/v1/water-rights
X-Tenant-Id: demo
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

```http
POST /api/v1/water-rights/{id}/transfers
X-Tenant-Id: demo
Authorization: Bearer ...
Content-Type: application/json

{
  "newOwnerId": "11111111-1111-1111-1111-111111111111",
  "transferDate": "2026-03-18",
  "transferReason": "SALE",
  "notes": "Transferencia por compraventa"
}
```

---

## Referencias

- [ADR-001 — Monolito modular multitenant](11-adr-001-monolith-modular.md)
- [Estado del backend](14-backend-status.md)
- [README del backend](../backend/README.md)

# RF-068 — Gestión de usuarios por alcance de tenant (plataforma y presidente)

| Campo | Valor |
|-------|-------|
| **ID** | RF-068 |
| **Tipo** | Historia de usuario |
| **Épica** | Seguridad · Gestión de tenants |
| **Requisitos relacionados** | RF-001, RF-002, RF-004, RNF-005 |
| **Depende de** | [RF-067](RF-067-tenant-create-with-president.md) (presidente inicial opcional pero recomendado) |
| **Estado actual** | `POST /users` acepta `tenantId`; UI no lo expone; sin restricción fuerte por actor |

---

## Modelo de roles (acordado)

| Actor | Rol | `tenantId` | Gestión de usuarios |
|-------|-----|------------|---------------------|
| Administrador de **plataforma** | `ADMIN` | `null` | Cualquier tenant; selector de `tenantId` en UI |
| Responsable del **acueducto** | `PRESIDENT` | fijo | Solo su tenant; **sin** selector de tenant |
| Operación | `TREASURY` | fijo | No gestiona usuarios |

El presidente crea principalmente usuarios `TREASURY` (y otros operativos acordados). **No** crea otro `ADMIN` de plataforma ni asigna usuarios a otros tenants.

---

## Historia de usuario

**Como** administrador de plataforma,  
**quiero** crear usuarios eligiendo el `tenantId` al que pertenecen,  
**para** dar de alta operativos en cualquier acueducto.

**Como** presidente del acueducto,  
**quiero** crear y administrar usuarios de tesorería (y roles operativos permitidos) de **mi** acueducto,  
**para** delegar la operación sin poder asignarlos a otro tenant.

---

## Contexto

La pantalla `/users` está restringida a `roleGuard(['ADMIN'])`, por lo que un `PRESIDENT` no accede hoy. El formulario no muestra `tenantId`. En backend, un cliente podría enviar otro `tenantId` si conoce la API.

---

## Alcance

### Backend

- **Administrador de plataforma** (`ADMIN`, `tenantId` null):
  - Puede crear usuarios con `tenantId` explícito (obligatorio salvo alta de otro admin de plataforma, fuera de alcance habitual).
  - Puede listar usuarios filtrando por `tenantId` (query param o endpoint dedicado).
- **Presidente** (`PRESIDENT`, `tenantId` no null):
  - `tenantId` del nuevo usuario = siempre el del JWT / contexto; ignorar o rechazar otro valor en body.
  - Solo roles permitidos al crear: `TREASURY`, `PRESIDENT` (opcional: ¿permitir segundo presidente? **por defecto: no**, solo `TREASURY`).
  - Listar/actualizar/desactivar solo usuarios del mismo `tenantId`.
- Respuestas ante violación: `403 Forbidden` o `400` con mensaje claro («No puede asignar usuarios a otro tenant»).
- Autorización en `UserController` / `UserService` según perfil del autenticado (no solo `@PreAuthorize("hasRole('ADMIN')")` global).

### Frontend (UI)

- Ruta `/users` accesible para `ADMIN` (plataforma) y `PRESIDENT`.
- **Admin plataforma** (`tenantId` null en sesión):
  - Selector **Tenant** obligatorio al crear usuario (lista de tenants activos).
  - Listado con columna `tenantId` o nombre del acueducto; filtro por tenant opcional.
  - Roles creables: `PRESIDENT`, `TREASURY` (no ofrecer alta de otro admin plataforma en UI).
- **Presidente**:
  - Sin selector de tenant; subtítulo indica el acueducto actual.
  - Rol por defecto `TREASURY`; sin opción `ADMIN`.
  - Listado solo usuarios del tenant de la sesión.
- Menú lateral: entrada «Usuarios» visible para `PRESIDENT` y admin plataforma.
- Alinear `AuthStore` / guards (`canManageUsers()` o similar) con las reglas anteriores.

---

## Criterios de aceptación

- [ ] Presidente accede a `/users` y crea un usuario `TREASURY` que queda con su mismo `tenantId`.
- [ ] Presidente no ve selector de tenant; intento API con otro `tenantId` falla.
- [ ] Admin plataforma ve selector de tenant y crea usuario en tenant B mientras opera sin `X-Tenant-Id` o con contexto de plataforma.
- [ ] Admin plataforma puede listar usuarios de un tenant elegido (filtro o listado global con columna tenant).
- [ ] Usuario `TREASURY` no accede a `/users`.
- [ ] Documentación de roles en historia alineada con RF-067 (presidente ≠ `ADMIN` con tenant).

---

## Fuera de alcance

- OTP / recuperación de contraseña por correo
- Auditoría detallada de altas de usuario (RNF-007 genérico)
- Edición masiva o importación CSV

---

## QA — pruebas manuales

1. Plataforma: crear tesorero en tenant A y otro en tenant B → cada uno con `tenantId` correcto.
2. Presidente de A: crear tesorero → `tenantId` A; llamada API con `tenantId` B → error.
3. Tesorero: navegar a `/users` → redirección o 403 según guard.
4. Plataforma: listado muestra usuarios de varios tenants con identificación clara.

---

## Referencias técnicas

- `auth/application/UserService.java` (`resolveTenantId`)
- `auth/interfaces/rest/controller/UserController.java`
- `frontend-angular/src/app/users/users.page.ts`
- `frontend-angular/src/app/core/auth/auth.store.ts` (`canManageTenants`)
- `frontend-angular/src/app/app.routes.ts` (`roleGuard` en `/users`)

# RF-067 — Crear tenant con presidente inicial (mismo formulario)

| Campo | Valor |
|-------|-------|
| **ID** | RF-067 |
| **Tipo** | Historia de usuario |
| **Épica** | Gestión de tenants · Seguridad |
| **Requisitos relacionados** | RF-001, RF-002, RF-004 |
| **Depende de** | API tenants, API usuarios (`auth`) |
| **Complementa** | [RF-068](RF-068-user-management-by-tenant-scope.md) |

---

## Modelo de roles (acordado)

| Actor en la historia | Rol en sistema | `tenantId` |
|----------------------|----------------|------------|
| Administrador de **plataforma** | `ADMIN` | `null` |
| Responsable del **acueducto** (admin del tenant) | `PRESIDENT` | id del tenant creado |

> No se usa `ADMIN` con `tenantId` asignado. El presidente del acueducto es siempre `PRESIDENT`.

---

## Historia de usuario

**Como** administrador de plataforma,  
**quiero** registrar un nuevo acueducto (tenant) y su presidente en un **mismo formulario**,  
**para** que el tenant quede operativo desde el primer día con un responsable que pueda crear usuarios de tesorería y demás roles.

---

## Contexto

Hoy `POST /api/v1/tenants` solo persiste el tenant. El primer usuario del acueducto debe crearse aparte, sin flujo guiado. El modal de tenants en Angular (`TenantUpsertDialogComponent`) no incluye datos del presidente.

---

## Alcance

### Backend

- Extender alta de tenant (nuevo DTO o campos anidados) con datos mínimos del presidente:
  - `fullName`, `email`, `password` (mín. 8 caracteres, misma regla que usuarios).
- Operación **transaccional**: si falla la creación del usuario, no debe quedar el tenant huérfano (rollback).
- Usuario creado: `roleCode = PRESIDENT`, `tenantId = id` del tenant, `status = ACTIVE`.
- Email único global (misma regla que `UserService.create`).
- Solo ejecutable por **administrador de plataforma** (`ADMIN` + `tenantId` null).

### Frontend (UI)

- En el modal **Nuevo tenant**, sección «Presidente del acueducto» con:
  - Nombre completo, correo, contraseña (y confirmación opcional en UI).
- Validaciones en formulario antes de enviar.
- Un solo submit crea tenant + presidente; mensajes de éxito/error claros.
- En modo **editar** tenant, no se exige re-capturar presidente (fuera de alcance).

---

## Criterios de aceptación

- [ ] El administrador de plataforma completa tenant + presidente en un solo modal y una sola acción «Crear».
- [ ] Tras éxito, existe el tenant `ACTIVE` y un usuario `PRESIDENT` con `tenantId` igual al `id` del tenant.
- [ ] Si el email del presidente ya existe, la operación falla y **no** persiste el tenant.
- [ ] El presidente puede iniciar sesión y su JWT incluye `tenantId` del acueducto.
- [ ] `./scripts/ci.sh` (o pipeline equivalente) en verde.

---

## Fuera de alcance

- Invitación por correo / contraseña temporal autogenerada
- Más de un presidente por tenant
- Crear presidente desde pantalla Usuarios (cubierto en RF-068)

---

## QA — pruebas manuales

1. Admin plataforma: crear tenant `demo-1` + presidente → login con ese correo → perfil con `tenantId=demo-1` y rol `PRESIDENT`.
2. Repetir email de presidente → error, tenant `demo-2` no debe existir si se reutiliza el mismo id en otro intento fallido del mismo request.
3. Usuario `PRESIDENT` de otro tenant no puede invocar el endpoint de alta con presidente (403).

---

## Referencias técnicas

- `tenants/application/TenantService.java`
- `auth/application/UserService.java`
- `frontend-angular/src/app/tenants/tenants.page.ts` (`TenantUpsertDialogComponent`)

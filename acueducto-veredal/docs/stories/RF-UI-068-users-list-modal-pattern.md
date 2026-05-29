# RF-UI-068 — Pantalla Usuarios: validar y alinear patrón listado + modales

| Campo | Valor |
|-------|-------|
| **Issue** | https://github.com/gustavosotocano/Acueducto/issues/43 |
| **ID** | RF-UI-068 |
| **Tipo** | Historia de usuario (frontend) |
| **Épica** | Seguridad · UX transversal |
| **Requisitos relacionados** | [RF-068](RF-068-user-management-by-tenant-scope.md) |
| **Depende de** | RF-068 mergeado (reglas de negocio API y guards); API `GET/POST/PUT /api/v1/users` |
| **Guía de diseño** | [`.cursor/rules/frontend-product-ux-architecture.md`](../../.cursor/rules/frontend-product-ux-architecture.md) |
| **Referencia de implementación** | [owners.page.ts](../../frontend-angular/src/app/owners/owners.page.ts), [tenants.page.ts](../../frontend-angular/src/app/tenants/tenants.page.ts) |
| **Estado actual** | Refactor en rama `rf-ui-068-users-list-modal-pattern` — listado + modales |

---

## Historia de usuario

**Como** responsable de producto / desarrollador frontend,  
**quiero** auditar la pantalla `/users` frente al patrón «solo listado + crear/modificar/eliminar en modal»,  
**para** mantener coherencia con el resto del backoffice y la guía de UX del proyecto.

**Como** administrador de plataforma o presidente del acueducto,  
**quiero** gestionar usuarios desde un listado claro con acciones en modales,  
**para** encontrar rápido quién existe y crear o actualizar sin scroll entre formulario y tabla.

---

## Contexto

Tras RF-068, `/users` expone correctamente el alcance por rol (selector de tenant para plataforma, presidente solo `TREASURY`, etc.), pero la UI sigue el patrón antiguo:

| Aspecto | Estado actual (`users.page.ts`) | Patrón esperado (guía UX) |
|---------|----------------------------------|---------------------------|
| Vista principal | Dos `mat-card`: «Nuevo usuario» + «Listado» | Un bloque de **listado** dominante |
| Crear | Formulario **inline** en la página | Botón «Nuevo» / `+` → **modal** con formulario |
| Modificar | No expuesto en UI (API `PUT` existe) | Acción por fila → **modal** edición (nombre, rol, estado; contraseña opcional o reset aparte) |
| Eliminar / desactivar | No expuesto en UI | Acción por fila → **modal** de confirmación → `PUT` con `status: INACTIVE` |
| Cabecera | `PageHeaderComponent` sin acciones `+` / refresh | Igual que Owners: acciones en header del listado |
| Móvil | Solo tabla | Cards en viewport estrecho (mejora alineada con guía) |

La validación de esta historia confirma brechas y, si no cumple, el desarrollo refactoriza sin cambiar reglas de RF-068.

---

## Alcance

### Fase 1 — Validación (obligatoria)

Checklist de auditoría documentado en el PR o comentario de la historia:

1. Revisar `frontend-angular/src/app/users/users.page.ts` frente a la guía UX.
2. Comparar con `owners.page.ts` (listado + `MatDialog` create/update + confirm deactivate).
3. Verificar que RF-068 sigue cumpliéndose tras cualquier cambio (roles, tenant, guards).

### Fase 2 — Ajuste UI (si la validación falla)

| Elemento | Comportamiento |
|----------|----------------|
| Ruta | `/users` (sin rutas hijas para CRUD) |
| Listado | Tabla (desktop) con columnas actuales + columna **Acciones** |
| Filtro | Mantener filtro por acueducto solo para admin plataforma (en toolbar del listado, no en card aparte de alta) |
| Crear | `UserUpsertDialogComponent` (o equivalente) en modo `create`; campos según actor (tenant/rol RF-068) |
| Modificar | Mismo dialog en modo `update`: `fullName`, `roleCode`, `status`; sin cambiar email si no hay API |
| Desactivar | Dialog de confirmación → `PUT` con `status: INACTIVE` |
| Reset contraseña | Opcional en menú de fila o submodal; `POST /users/{id}/reset-password` |
| Post-acción | Cerrar modal, snackbar, `refresh$` del listado |

**Fuera de alcance**

- Cambiar reglas de autorización de RF-068 en backend
- Alta de admin de plataforma desde esta pantalla
- Búsqueda textual / paginación server-side (mejora futura)
- OTP / recuperación de contraseña por correo

---

## Criterios de aceptación

### Validación

- [ ] Existe informe breve (en PR o sección «Resultado validación» abajo) con ítems **Cumple** / **No cumple** por fila de la tabla de contexto.
- [ ] Si hay ítems **No cumple**, queda plan de refactor (esta misma historia, fase 2) estimado en el PR.

### Cumplimiento del patrón (fase 2, cuando aplique)

- [ ] La ruta `/users` muestra **solo** el listado como contenido principal (sin `mat-card` de formulario de creación visible).
- [ ] Botón agregar en header o toolbar abre **modal** de creación; al guardar se refresca el listado.
- [ ] Cada fila (o card en móvil) tiene acción **Editar** que abre modal con datos del usuario.
- [ ] Acción **Desactivar** (o equivalente) abre modal de confirmación y persiste `INACTIVE`.
- [ ] No queda formulario de alta/edición fuera de modales en esta ruta.
- [ ] Patrón visual coherente con Owners (`PageHeaderComponent`, iconos `+` / `refresh` si aplica).
- [ ] **Regresión RF-068:** presidente sin selector tenant; plataforma con selector en modal de alta; tesorero sin acceso a `/users`.

---

## Resultado de validación (rellenar en dev/QA)

| Ítem | Cumple | Notas |
|------|--------|-------|
| Solo listado en página | Sí | Sin card de formulario inline |
| Crear en modal | Sí | `UserUpsertDialogComponent` + botón `+` |
| Editar en modal | Sí | Mismo dialog modo `update` |
| Desactivar en modal | Sí | `UsersConfirmDialogComponent` → `INACTIVE` |
| Coherencia con Owners/Tenants | Sí | Header acciones, columna Acciones |
| RF-068 funcional | Sí | Sin cambios backend; reglas en modales |

**Veredicto:** ☑ Cumple patrón · ☐ Requiere refactor (fase 2)

---

## QA — pruebas manuales

**QA scope:** `Frontend`

1. Admin plataforma: abrir `/users` → solo listado + botón nuevo; crear tesorero en tenant A desde modal → aparece en tabla.
2. Presidente: sin selector tenant en modal de alta; crear tesorero; no accede tesorero a `/users`.
3. Editar usuario: cambiar nombre o estado desde modal → tabla actualizada.
4. Desactivar usuario: confirmar en modal → estado `INACTIVE` en listado.
5. Desktop y ancho &lt; 768px: listado usable (tabla o cards).
6. Error API (email duplicado, rol no permitido) → snackbar; modal permanece abierto o permite corregir.

---

## Referencias técnicas

- `frontend-angular/src/app/users/users.page.ts`
- `frontend-angular/src/app/users/users.service.ts`
- `frontend-angular/src/app/core/auth/auth.store.ts` — `canManageUsers()`, `canManageTenants()`
- `docs/stories/RF-068-user-management-by-tenant-scope.md`

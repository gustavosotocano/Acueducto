# RF-UI-051 — Pantalla «Nueva factura»: listado y modal de creación

| Campo | Valor |
|-------|-------|
| **ID** | RF-UI-051 |
| **Tipo** | Historia de usuario (frontend) |
| **Épica** | Facturación |
| **Requisitos relacionados** | RF-051, RF-052, RF-053, RF-055, RF-056, RF-057 |
| **Depende de** | API `POST/GET /api/v1/invoices`, `POST /api/v1/invoices/{id}/status` |
| **Complementa** | [RF-UI-060](RF-UI-060-invoices-monthly-batch-screen.md) |

---

## Historia de usuario

**Como** usuario de tesorería u operación del acueducto,  
**quiero** ver solo el listado de facturas en la sección «Nueva factura» y crear o gestionar facturas desde acciones claras (agregar en modal, modificar estado),  
**para** no mezclar la facturación manual con el lote mensual y seguir el mismo patrón de otras pantallas (Owners, Properties).

---

## Contexto

La pantalla `/invoices` concentraba en una sola vista: lote mensual, formulario de creación y tabla. Eso dificultaba el flujo diario y no alineaba con RF-056 (factura emitida no editable en sus datos; ajustes vía estado/anulación).

---

## Alcance

| Elemento | Comportamiento |
|----------|----------------|
| Ruta | `/invoices/manual` (entrada por defecto al abrir Invoices) |
| Submenú | Pestañas bajo Invoices: «Nueva factura» \| «Factura mensual» |
| Listado | Tabla con número, estado, emisión, vence, total |
| Agregar | Botón `+` abre **modal** con formulario de creación manual |
| Modificar | Botón editar abre **modal** de cambio de estado (no edición de montos/fechas, RF-056) |
| API | `POST /invoices`, `POST /invoices/{id}/status` |

Fuera de alcance en esta historia:

- Edición de campos de factura ya emitida (PUT/PATCH)
- Notas crédito/débito (RF-058)
- Filtros avanzados en listado (RF-052 parcial; mejora futura)
- Consecutivo automático en UI (RF-055; el usuario aún puede indicar número si el backend lo exige)

---

## Criterios de aceptación

- [ ] Al navegar a Invoices, se muestra el submenú con «Nueva factura» y «Factura mensual».
- [ ] «Nueva factura» muestra **solo** el listado (sin formulario inline ni bloque de lote).
- [ ] El botón agregar abre un modal; al confirmar se llama `POST /invoices` y se refresca el listado con snackbar de éxito/error.
- [ ] El botón modificar abre modal de **cambio de estado** con estados permitidos (`DRAFT`, `ISSUED`, `PAID`, `OVERDUE`, `CANCELLED`) y nota opcional; persiste vía `POST /invoices/{id}/status`.
- [ ] No existe formulario de creación visible fuera del modal en esta ruta.
- [ ] Patrón visual coherente con `PageHeaderComponent` y acciones `+` / `refresh` como Owners.

---

## QA — pruebas manuales

1. Abrir `/invoices` → redirige a `/invoices/manual` con listado.
2. Crear factura desde modal con water right válido → aparece en tabla.
3. Cambiar estado a `CANCELLED` con motivo → estado actualizado en tabla.
4. Error de API (número duplicado, tenant sin seleccionar) → mensaje en snackbar, modal puede cerrarse o corregirse.
5. Cambiar a pestaña «Factura mensual» → no se ve listado ni modal de creación en esa vista.

---

## Referencias

- `frontend-angular/src/app/invoices/invoices-manual.page.ts`
- `frontend-angular/src/app/invoices/invoices-shell.page.ts`
- `docs/acueducto-veredal-requerimientos.md` — RF-051 a RF-057

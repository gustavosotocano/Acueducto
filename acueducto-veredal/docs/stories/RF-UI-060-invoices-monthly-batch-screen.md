# RF-UI-060 — Pantalla «Factura mensual» (lote RF-060)

| Campo | Valor |
|-------|-------|
| **ID** | RF-UI-060 |
| **Tipo** | Historia de usuario (frontend) |
| **Épica** | Facturación |
| **Requisitos relacionados** | RF-060, RF-065 |
| **Depende de** | API `POST /api/v1/invoices/batch/monthly` |
| **Complementa** | [RF-UI-051](RF-UI-051-invoices-manual-list-modal.md) |

---

## Historia de usuario

**Como** usuario de operación del acueducto,  
**quiero** una pantalla dedicada solo a la facturación mensual por lote,  
**para** ejecutar RF-060 sin distracciones del listado ni del alta manual.

---

## Contexto

El lote mensual compartía la misma página que la creación manual y el listado. La operación de fin de mes requiere foco en año/mes, fechas opcionales y resultado (creadas/omitidas).

---

## Alcance

| Elemento | Comportamiento |
|----------|----------------|
| Ruta | `/invoices/monthly` |
| Submenú | Segunda pestaña bajo Invoices |
| Formulario | Año, mes, fechas opcionales de emisión/vencimiento |
| Acción | «Ejecutar lote mensual» → `POST /invoices/batch/monthly` |
| Resultado | Resumen inline (creadas/omitidas, periodo) y tabla de omitidas con motivo |

Fuera de alcance:

- Listado de todas las facturas en esta vista
- Modal de creación manual
- Programación automática del batch (cron)

---

## Criterios de aceptación

- [ ] La pestaña «Factura mensual» muestra únicamente el flujo de lote (sin listado ni formulario de factura manual).
- [ ] Tras ejecutar el lote, se muestra conteo de creadas/omitidas y detalle de skips si aplica.
- [ ] Errores de API se muestran en snackbar.
- [ ] Validación de formulario: año y mes requeridos en rango válido.

---

## QA — pruebas manuales

1. Ir a `/invoices/monthly`, ejecutar lote para mes actual → resumen visible.
2. Re-ejecutar mismo periodo → omitidas por duplicado (según reglas backend).
3. Verificar que en «Nueva factura» el listado no incluye el formulario de lote.

---

## Referencias

- `frontend-angular/src/app/invoices/invoices-monthly.page.ts`
- `docs/acueducto-veredal-requerimientos.md` — RF-060

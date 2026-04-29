## Frontend: contexto, UX/UI y arquitectura (guía)

Esta regla es guía permanente para construir el frontend de la aplicación transaccional del **acueducto veredal**.

---

## Contexto del negocio
- La aplicación administra: `owners` (propietarios), `properties` (predios), `water_rights` (derechos de agua), facturas, pagos y cartera.
- El **core del negocio** es `water_right`.
- Un `owner` puede tener múltiples `water_rights`.
- Todo el proceso de **facturación**, **pagos** y **cartera** gira alrededor de `water_right`.
- Los `water_rights` pueden transferirse entre owners manteniendo historial.
- Debe funcionar bien en **desktop** y **móvil**.

---

## Stack deseado
- Angular moderno
- Standalone components
- Lazy loading por feature
- Angular Material como base UI
- Tailwind CSS para mejorar estética visual
- Reactive Forms
- Signals para estado local cuando aplique
- Consumo de APIs REST desde backend Spring Boot

---

## Objetivo del frontend
Construir una app funcional, moderna y agradable visualmente (no solo CRUD). El diseño debe ayudar a que el usuario quiera usarla.

---

## Lineamientos de UX/UI
- Estilo backoffice moderno / SaaS administrativo.
- Sidebar en desktop.
- Topbar con usuario, título de sección y acciones principales.
- En móvil: navegación adaptada y **tablas convertidas en cards**.
- Tablas con búsqueda, filtros, paginación y acciones claras.
- Formularios limpios, agrupados por secciones y con validación visible.
- Uso de colores con propósito:
  - Azul: acciones principales / confianza.
  - Verde: pagos realizados / estados correctos.
  - Amarillo: pendientes.
  - Rojo: deudas vencidas / errores.
  - Grises suaves: fondos / separación visual.
- Evitar interfaz plana/vieja o demasiado cargada.
- Priorizar claridad, facilidad de uso y buena presentación visual.

---

## Arquitectura sugerida
`src/app`
  - `core/`
    - `auth/`
    - `interceptors/`
    - `layout/`
    - `guards/`
  - `shared/`
    - `components/`
    - `pipes/`
    - `validators/`
    - `ui/`
  - `features/`
    - `owners/`
    - `properties/`
    - `water-rights/`
    - `invoices/`
    - `payments/`
    - `debt/`
    - `dashboard/`

---

## Forma de trabajo
Ayudar a diseñar y construir el frontend paso a paso, empezando por:
1) arquitectura base
2) layout principal
3) sistema visual
4) componentes reutilizables
5) features (una a una)

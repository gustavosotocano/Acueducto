# Acueducto Veredal

Sistema para la administración de uno o varios acueductos veredales, orientado al control de dueños de derechos de agua, facturación mensual por derecho, cartera, gastos, cierres mensuales y gestión administrativa.

## Objetivo

Digitalizar y organizar la operación del acueducto veredal mediante una plataforma web con trazabilidad, seguridad y capacidad de crecimiento hacia pagos electrónicos.

## Principios del dominio

- El **derecho de agua** es la unidad central del negocio.
- Una persona puede tener **uno o varios derechos de agua**.
- La **facturación** y la **cartera** se generan **por derecho de agua**.
- El sistema debe permitir **consulta consolidada por persona**.
- Un derecho de agua puede **transferirse** a otro propietario **sin perder su identificador**.
- El sistema debe conservar el **historial de propietarios** y el **historial del inmueble o dirección asociada**.
- La aplicación debe soportar **múltiples acueductos veredales** mediante un enfoque **multitenant**.

## Alcance del MVP

- Registro de acueductos veredales como tenants
- Registro de dueños de derecho de agua
- Gestión de derechos de agua
- Transferencia de derechos entre propietarios
- Historial de propietarios e inmueble asociado
- Parametrización de tarifas
- Generación de facturación mensual por derecho de agua
- Registro de pagos y recibos de caja
- Control de cartera por derecho y consolidada por persona
- Registro de gastos
- Cierre mensual básico
- Autenticación y roles

## Arquitectura propuesta

- Frontend: Angular
- Backend: Spring Boot
- Base de datos: PostgreSQL
- Infraestructura local: Docker Compose
- Estilo arquitectónico: Monolito modular multitenant

## Estructura del repositorio

- `docs/`: especificación funcional y técnica
- `backend/`: servicio backend en Spring Boot
- `frontend-angular/`: aplicación frontend en Angular
- `infra/`: infraestructura local y despliegue

## Documentación inicial

- `docs/sdlc.md` — flujo de desarrollo, QA y verificaciones (`./scripts/ci.sh`)
- `docs/phrases-to-start.md` — frases para operar con el agente
- `docs/01-vision.md`
- `docs/05-business-rules.md`
- `docs/06-domain-model.md`
- `docs/13-mvp-backlog.md`
- `docs/acueducto-veredal-requerimientos.md`

## Roadmap

### Fase 1
- Core administrativo
- Derechos de agua
- Facturación
- Cartera
- Gastos
- Cierre mensual

### Fase 2
- Integración con WhatsApp
- Portal del usuario
- Pagos electrónicos

## Convenciones

- Commits pequeños y claros
- Pull requests con contexto funcional
- Historias de usuario trazables a issues
- Cambios de arquitectura documentados con ADR

## Estado del proyecto

Fase de definición y estructuración inicial bajo enfoque spec-driven design.

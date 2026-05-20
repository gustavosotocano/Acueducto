## Phrases to start (Dev / QA / Bugs)

Este documento define frases cortas para disparar procesos con el agente, sin depender de memoria.

Flujo completo: [`docs/sdlc.md`](./sdlc.md).

### Convenciones
- Reemplaza `<xxx>` por el número de RF (ej: `065`).
- Reemplaza `#<n>` por el número de PR o issue.
- Estados del Project #7: `Ready` → `In progress` → `Code review` → `In review` → `Done`.
- **QA scope** define dónde se prueba: `API` | `Frontend` | `Both`.

---

## Dev (desarrollo)

- **Dev: toma la RF-<xxx>**
  - **Obligatorio:** mover card en Project #7 de `Ready` → `In progress` (al iniciar)
  - `git pull` de `main`; crear rama `rf-<xxx>-...`
  - Implementar; antes del PR ejecutar `./scripts/ci.sh` (ver `docs/sdlc.md`)
  - Al abrir PR (plantilla completa, CI verde): **mover card** `In progress` → `Code review`

- **Dev: crea PR para RF-<xxx>**
  - (Si ya existe rama) crear PR con plantilla (Summary / QA scope / test plan / Pasos QA)
  - **Obligatorio:** mover card a `Code review` (no dejar en `In progress`)

- **Dev: revisa el PR #<n>**
  - Code review: diff, arquitectura, tests, multitenant
  - **Approve** o **Request changes**
  - Si OK: **mover card** `Code review` → `In review` (handoff QA)
  - Si cambios: **mover card** `Code review` → `In progress` hasta re-review

- **Dev: aplica fixes del PR #<n>**
  - Card en `In progress` (si salió de code review con cambios)
  - Trabajar sobre la rama del PR; `./scripts/ci.sh` en verde
  - Al reenviar: mover card otra vez a `Code review`

- **Dev: mergea el PR #<n> (si QA aprobó)**
  - Merge por defecto: **Squash**
  - **Obligatorio:** mover card `In review` → `Done`

---

## QA (calidad)

- **QA scope (referencia)**
  - `API`: validación por endpoints (Postman/curl), sin UI.
  - `Frontend`: validación por pantallas (sin usar endpoints directo).
  - `Both`: combina UI + validaciones puntuales por API.

- **QA: valida el PR #<n>**
  - Solo si el PR ya tiene **code review aprobado** (Approve o `CODE REVIEW PASS`)
  - Ejecuta smoke + pruebas funcionales + regresión (según módulo)
  - Si pasa: deja **QA PASS** (Approve si es posible; si eres el autor del PR, deja comentario)
  - Si falla: reporta bugs (1 issue por bug)

- **QA: valida la historia RF-<xxx> (PR #<n>)**
  - Valida criterios de aceptación de la RF/issue contra el PR

- **QA: revalida el PR #<n> (fixes)**
  - Repite pruebas enfocadas en lo corregido + smoke

- **QA: reporta bugs del PR #<n>**
  - Crea **1 issue por bug** con plantilla
  - Los agrega al Project en `Ready`
  - Los enlaza al PR y a la RF/historia original

- **QA: reporta bugs (severidad: blocker|major|minor) del PR #<n>**
  - Igual, agregando label de severidad

---

## Bugs (gestión)

- **Bugs: lista los bugs abiertos de RF-<xxx>**
  - Muestra issues de bugs relacionados (links) para priorizar

- **Bugs: deja en Ready los bugs nuevos del PR #<n>**
  - Asegura que todos los bugs creados estén en `Ready` (Project #7)

---

## Ejemplos rápidos

- `Dev: toma la RF-065`
- `QA: valida el PR #10`
- `QA: reporta bugs (severidad: major) del PR #10`

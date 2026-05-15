## Phrases to start (Dev / QA / Bugs)

Este documento define frases cortas para disparar procesos con el agente, sin depender de memoria.

Flujo completo: [`docs/sdlc.md`](./sdlc.md).

### Convenciones
- Reemplaza `<xxx>` por el número de RF (ej: `065`).
- Reemplaza `#<n>` por el número de PR o issue.
- Estados del Project #7: `Ready` → `In progress` → `In review` → `Done`.
- **QA scope** define dónde se prueba: `API` | `Frontend` | `Both`.

---

## Dev (desarrollo)

- **Dev: toma la RF-<xxx>**
  - Crea rama `rf-<xxx>-...`
  - Mueve la historia de `Ready` → `In progress`
  - Implementa; antes del PR ejecuta `./scripts/ci.sh` (ver `docs/sdlc.md`)
  - Crea PR (plantilla con QA scope / test plan / pasos QA) y mueve la historia a `In review`

- **Dev: crea PR para RF-<xxx>**
  - (Si ya existe rama) crea el PR con plantilla (Summary / QA scope / Test plan / Pasos QA)
  - Mueve la historia a `In review`

- **Dev: aplica fixes del PR #<n>**
  - Trabaja sobre la rama del PR
  - Empuja commits de corrección y vuelve a ejecutar `./scripts/ci.sh` en verde

- **Dev: mergea el PR #<n> (si QA aprobó)**
  - Merge por defecto: **Squash**
  - Mueve la historia a `Done`

---

## QA (calidad)

- **QA scope (referencia)**
  - `API`: validación por endpoints (Postman/curl), sin UI.
  - `Frontend`: validación por pantallas (sin usar endpoints directo).
  - `Both`: combina UI + validaciones puntuales por API.

- **QA: valida el PR #<n>**
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

## Regla: Flujo de QA (estado `In review`) y reporte de bugs

Esta regla define cómo operar cuando una historia llega a **`In review`** en el GitHub Project `gustavosotocano` #7.

### Objetivo
- En `In review`, **QA/Calidad** valida la historia (funcional + regresión).
- Si pasa, se hace merge y se mueve a **`Done`**.
- Si falla, QA reporta **bugs** como issues para que desarrollo los tome (y la historia principal permanece en `In review` hasta revalidación).

---

## 1) Entrada a `In review` (handoff)
QA solo empieza cuando el PR tiene **code review aprobado** (GitHub Approve de un revisor distinto al autor, o comentario `CODE REVIEW PASS` documentado en equipo pequeño).

Cuando desarrollo mueva una historia a `In review`, el PR debe incluir obligatoriamente:
- **Summary** (qué se hizo)
- **QA scope**: `API` | `Frontend` | `Both`
- **Test plan (dev)** (qué se probó en desarrollo)
- **Pasos para QA** (pasos concretos que QA debe ejecutar)
- **Criterios de aceptación** (checklist verificable)
- **Referencias**: issue RF/bug, PR relacionado, endpoints/pantallas si aplica

Regla: **No merge** hasta que QA haya ejecutado pruebas y dado aprobación.

### Nota sobre aprobación (GitHub)
GitHub **no permite aprobar tu propio PR**. Si el autor del PR es la misma cuenta que ejecuta QA, entonces:
- No intentar `Approve`.
- Dejar un comentario explícito en el PR con resultado:
  - **QA PASS** (con evidencia / comandos / casos ejecutados), o
  - **QA FAIL** (con bugs creados y links).

---

## 2) Ejecución de QA
QA debe ejecutar al menos:
- **Smoke test**: no rompe lo básico y la app levanta/funciona.
- **Pruebas funcionales**: valida criterios de aceptación del issue/RF.
- **Regresión**: validar flujos cercanos (según módulo afectado: invoices/payments/portfolio/water-rights/owners/properties).

---

## 3) Criterio de salida a `Done`
Una historia solo puede pasar a `Done` cuando:
- QA dejó **aprobación** (review “Approve” o comentario explícito de OK / **QA PASS**).
- El PR fue **mergeado**.
- Se confirmó verificación mínima post-merge (según el contexto: build/CI o validación manual).

---

## 4) Si QA encuentra un bug
QA debe:
- **No aprobar** el PR mientras el bug esté presente.
- Crear **1 issue por bug** (no agrupar bugs distintos en un solo issue).
- Enlazar el bug con:
  - el PR
  - la historia/RF original

Los bugs deben entrar al Project en **`Ready`** para que desarrollo los tome.

### Labels sugeridos
- `bug`
- `qa`
- `backend` / `frontend`
- `facturacion` (u otros del dominio)
- severidad: `blocker` / `major` / `minor`

---

## 5) Plantilla estándar para bug report
Usar este formato en el body del issue:

- `## Contexto`
- `## Pasos para reproducir`
- `## Resultado esperado`
- `## Resultado actual`
- `## Evidencia`
- `## Severidad`
- `## Referencias` (PR, RF/Historia, logs, screenshots)

Ejemplo:

```md
## Contexto
Al probar RF-XXX en el PR #NN.

## Pasos para reproducir
1. ...
2. ...

## Resultado esperado
...

## Resultado actual
...

## Evidencia
Captura / logs / video.

## Severidad
major

## Referencias
- PR: <link>
- Historia: <link>
```

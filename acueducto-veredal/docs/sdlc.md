# SDLC — Acueducto Veredal

Flujo de desarrollo, calidad y entrega. Pensado para operar con **GitHub Project #7** y un agente (frases en `phrases-to-start.md`). Las verificaciones de código son **portables** (no dependen de GitHub Actions).

## Estados del board

`Ready` → `In progress` → `Code review` → `In review` → `Done`

| Estado | Responsable | Qué ocurre |
|--------|-------------|------------|
| Ready | — | Historia priorizada, lista para tomar |
| In progress | Dev | Rama `rf-XXX-<slug>`, implementación, PR abierto, `./scripts/ci.sh` en verde |
| Code review | Dev (revisor) | Revisión del **diff** en GitHub: diseño, reglas de negocio, multitenant, tests; **Approve** o **Request changes** |
| In review | QA | Solo después de **code review aprobado**; pruebas funcionales y regresión |
| Done | Dev (post-merge) | **QA PASS** + PR mergeado (squash) |

### Dos revisiones distintas (no mezclar)

| Tipo | Pregunta que responde | Cuándo | Señal de salida |
|------|----------------------|--------|-----------------|
| **Code review** | ¿El código está bien escrito, coherente y mantenible? | **Antes** de QA | ≥1 **Approve** en el PR (quien no sea el autor) o checklist explícito si equipo de 1 persona |
| **QA** | ¿El producto cumple la RF y no rompe regresión? | **Después** de code review | Comentario **QA PASS** (o Approve si aplica) + merge |

**Reglas:**

- No mover a **`In review`** (QA) hasta: CI verde + **code review aprobado**.
- La historia permanece en **`In review`** hasta **QA PASS** y merge.
- Si hay bugs de QA, issues aparte en `Ready`; no pasar a `Done` con bugs abiertos de esa entrega.
- Si code review pide cambios, el PR vuelve a **`In progress`** (o permanece ahí) hasta re-review.

## Verificación de código (portable)

Antes de solicitar **code review**, el dev ejecuta según el alcance:

```bash
./scripts/ci.sh all          # backend + frontend
./scripts/ci.sh backend      # solo API
./scripts/ci.sh frontend     # solo UI
```

| Scope | Comando | Qué valida |
|-------|---------|------------|
| API | `./scripts/ci.sh backend` | `mvn test` |
| Frontend | `./scripts/ci.sh frontend` | `npm ci` + `npm run build` |
| Both | `./scripts/ci.sh all` | Ambos |

**CI en la nube (opcional):** en GitHub Actions, Bitbucket Pipelines u otro, el pipeline solo debe invocar `./scripts/ci.sh` con el target adecuado. La lógica vive en el script, no en el YAML del proveedor.

> **Backend:** Docker debe estar **en ejecución** antes de `./scripts/ci.sh backend` (Testcontainers levanta PostgreSQL). Si Docker está apagado, el script falla con un mensaje explícito.

### Ejemplo: pipeline en la nube (opcional)

GitHub Actions (`.github/workflows/ci.yml`):

```yaml
on: [pull_request, push]
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21" }
      - uses: actions/setup-node@v4
        with: { node-version: "22" }
      - run: chmod +x ./scripts/ci.sh && ./scripts/ci.sh all
```

Bitbucket Pipelines (`bitbucket-pipelines.yml`):

```yaml
pipelines:
  pull-requests:
    "**":
      - step:
          image: maven:3.9-eclipse-temurin-21
          script:
            - apt-get update && apt-get install -y nodejs npm
            - chmod +x ./scripts/ci.sh
            - ./scripts/ci.sh all
```

En ambos casos el contrato es el mismo: **el pipeline llama al script**, no duplica comandos.

## Code review (antes de QA)

Checklist mínimo del revisor (GitHub **Files changed** + comentarios):

- [ ] El cambio responde a la RF/issue (sin scope creep innecesario)
- [ ] `./scripts/ci.sh` verde (o pipeline equivalente)
- [ ] Convenciones del repo (multitenant, soft delete, trazabilidad si aplica)
- [ ] Tests cubren el comportamiento nuevo o regresión relevante
- [ ] Sin secretos, sin `console.log` / código muerto evidente
- [ ] Documentación actualizada si cambió contrato o arquitectura

**En GitHub:** usar **Review → Approve** o **Request changes**. Si hay cambios, el dev pushea en la misma rama y se repite code review.

**Equipo de una persona:** hacer auto-revisión con la checklist en el PR y dejar comentario `CODE REVIEW PASS (self)` antes de pasar a QA; idealmente usar agente (`Dev: revisa el PR #n`) como segunda pasada.

**Handoff a QA:** mover la historia a **`In review`** solo cuando el PR tenga aprobación de código (o `CODE REVIEW PASS` documentado).

## Pull request (handoff a QA)

La plantilla en `.github/pull_request_template.md` exige:

- Summary
- **QA scope** (`API` | `Frontend` | `Both`)
- **Test plan (dev)** — incluir resultado de `./scripts/ci.sh`
- **Pasos para QA**
- **Criterios de aceptación** (checklist)
- Referencias (issue RF, endpoints, pantallas)

**No merge** sin **code review aprobado** + **QA PASS**. Si el autor del PR es quien hace QA, dejar comentario **QA PASS** o **QA FAIL** (GitHub no permite auto-aprobación).

## Bugs

- **1 issue por bug** — plantilla: `.github/ISSUE_TEMPLATE/bug_report.md`
- Labels sugeridos: `bug`, `qa`, `blocker`|`major`|`minor`, `backend`|`frontend`
- Agregar al Project en `Ready`
- Enlazar al PR y a la RF original

## Requerimientos → backlog

1. RFs en `docs/acueducto-veredal-requerimientos.md`
2. Crear issue por RF (plantilla `rf_story.md`) y agregar al Project #7
3. Regla del agente: `.cursor/rules/github-rf-to-project.md`

## Frases para el agente

Ver [`phrases-to-start.md`](./phrases-to-start.md) — ejemplos:

- `Dev: toma la RF-065`
- `Dev: revisa el PR #10` (code review)
- `QA: valida el PR #10` (solo después de code review)
- `QA: reporta bugs (severidad: major) del PR #10`

## Reglas Cursor (referencia)

| Archivo | Contenido |
|---------|-----------|
| `.cursor/rules/github-project-workflow.md` | Dev: ramas, estados, merge |
| `.cursor/rules/github-qa-workflow.md` | QA en `In review`, bugs |
| `.cursor/rules/github-rf-to-project.md` | RF → issues + Project |

## Documentación relacionada

- `docs/acueducto-veredal-requerimientos.md` — requerimientos (RF)
- `docs/13-mvp-backlog.md` — épicas MVP
- `docs/14-backend-status.md` — estado de implementación backend
- `docs/05-business-rules.md` — reglas de negocio

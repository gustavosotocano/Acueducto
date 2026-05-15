# SDLC — Acueducto Veredal

Flujo de desarrollo, calidad y entrega. Pensado para operar con **GitHub Project #7** y un agente (frases en `phrases-to-start.md`). Las verificaciones de código son **portables** (no dependen de GitHub Actions).

## Estados del board

`Ready` → `In progress` → `In review` → `Done`

| Estado | Responsable | Qué ocurre |
|--------|-------------|------------|
| Ready | — | Historia priorizada, lista para tomar |
| In progress | Dev | Rama `rf-XXX-<slug>`, implementación |
| In review | QA | PR abierto; pruebas funcionales y regresión |
| Done | Dev (post-merge) | QA aprobó, PR mergeado (squash) |

**Regla:** la historia permanece en `In review` hasta **QA PASS** y merge. Si hay bugs, se crean issues aparte en `Ready`; no mover la RF a `Done` con bugs abiertos de esa entrega.

## Verificación de código (portable)

Antes de pasar una historia a `In review`, ejecutar según el alcance:

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

## Pull request (handoff a QA)

La plantilla en `.github/pull_request_template.md` exige:

- Summary
- **QA scope** (`API` | `Frontend` | `Both`)
- **Test plan (dev)** — incluir resultado de `./scripts/ci.sh`
- **Pasos para QA**
- **Criterios de aceptación** (checklist)
- Referencias (issue RF, endpoints, pantallas)

**No merge** sin aprobación de QA. Si el autor del PR es quien hace QA, dejar comentario **QA PASS** o **QA FAIL** (GitHub no permite auto-aprobación).

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
- `QA: valida el PR #10`
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

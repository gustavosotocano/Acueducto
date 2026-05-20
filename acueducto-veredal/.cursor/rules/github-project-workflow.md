## Regla: Workflow de desarrollo por historia (GitHub Project #7)

Cuando el usuario diga “tomemos la RF-XXX y desarrollemosla” (o “tomemos la historia X”), seguir este flujo end-to-end en GitHub:

### Project
- **Project**: `gustavosotocano` Project **#7**
- **Estados**: `Ready` → `In progress` → `Code review` → `In review` → `Done`
- Se **inicia** tomando ítems que estén en **`Ready`**.

### Branching y PR
- **1 rama por historia**.
  - Siempre haz git pull de la rama main antes de crear la nueva rama
  - Nombre sugerido: `rf-XXX-<slug-corto>` (ej: `rf-055-consecutivo-factura`)
- Abrir **PR** hacia la rama principal del repo (la que corresponda: `main`/`master`).

### Cambios de estado en el Project (automatizar en el flujo)
1. Al iniciar trabajo:
   - Mover la historia a **`In progress`**.
2. Al abrir PR con CI verde y revisor asignado:
   - Mover a **`Code review`**.
3. Tras **Approve** de code review (revisor ≠ autor):
   - Mover a **`In review`** (handoff a QA).
4. Tras merge y verificación:
   - Mover a **`Done`**.

### Calidad
- **`Code review`**: revisión del diff (diseño, tests, multitenant). Salida: **Approve** o **Request changes** en GitHub.
- **`In review` es solo QA / Calidad**: empieza **después** de code review aprobado.
- Antes de `Code review` (dev):
  - ejecutar `./scripts/ci.sh` con el scope adecuado (`backend` | `frontend` | `all`)
  - completar plantilla del PR: **test plan (dev)**, **pasos para QA**, **criterios de aceptación**, **QA scope**
- No mover a **`In review`** sin code review aprobado.
- No hacer **merge** hasta **code review aprobado** + **QA PASS**.
- La historia **permanece en `In review`** hasta QA PASS + merge; no pasar a `Done` con bugs abiertos de esa entrega.

### Revisión y comentarios
- Responder comentarios del PR y hacer commits de ajuste en la misma rama.
- Mantener verificaciones en verde (`./scripts/ci.sh` local o pipeline del proveedor que invoque el mismo script).

### Estrategia de merge (default)
- Usar **Squash merge** (1 commit final por PR) salvo que el usuario indique otra estrategia.

### Notas operativas
- Si faltan scopes para manipular Projects:
  - `gh auth refresh --hostname github.com -s read:project`
  - `gh auth refresh --hostname github.com -s project`

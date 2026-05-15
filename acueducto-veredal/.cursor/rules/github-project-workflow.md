## Regla: Workflow de desarrollo por historia (GitHub Project #7)

Cuando el usuario diga “tomemos la RF-XXX y desarrollemosla” (o “tomemos la historia X”), seguir este flujo end-to-end en GitHub:

### Project
- **Project**: `gustavosotocano` Project **#7**
- **Estados**: `Ready` → `In progress` → `In review` → `Done`
- Se **inicia** tomando ítems que estén en **`Ready`**.

### Branching y PR
- **1 rama por historia**.
  - Siempre haz git pull de la rama main antes de crear la nueva rama
  - Nombre sugerido: `rf-XXX-<slug-corto>` (ej: `rf-055-consecutivo-factura`)
- Abrir **PR** hacia la rama principal del repo (la que corresponda: `main`/`master`).

### Cambios de estado en el Project (automatizar en el flujo)
1. Al iniciar trabajo:
   - Mover la historia a **`In progress`**.
2. Al abrir PR listo para revisión:
   - Mover a **`In review`**.
3. Tras merge y verificación:
   - Mover a **`Done`**.

### Calidad
- **`In review` es etapa de QA / Calidad**: a partir de ahí el área de calidad ejecuta pruebas y valida criterios de aceptación.
- Antes de pasar a `In review` (responsabilidad de desarrollo):
  - ejecutar `./scripts/ci.sh` con el scope adecuado (`backend` | `frontend` | `all`) según QA scope del PR
  - completar la plantilla del PR: **test plan (dev)**, **pasos para QA**, **criterios de aceptación**, **QA scope**
- No hacer **merge** hasta tener **aprobación de QA** (y revisión técnica si aplica).
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

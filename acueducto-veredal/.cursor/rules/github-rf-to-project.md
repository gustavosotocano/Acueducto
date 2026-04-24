## Regla: RF → GitHub Issues + Project

Cuando el usuario pida “crear RF como historias del backlog”, “pasar RF a GitHub”, “crear issues por RF”, o similar, sigue este playbook.

### Objetivo
- **Crear 1 GitHub Issue por RF**.
- **Agregar cada issue** al **GitHub Project** `gustavosotocano` **#7**.
- Asegurar que el **body** quede en **Markdown correcto**, con saltos de línea reales (**no** `\n` literales).

### Defaults
- **Repo**: detectar desde `git remote -v` (por defecto `origin`). Si no hay remoto, pedir el repo `OWNER/REPO`.
- **Project**: `gustavosotocano` Project **#7**.
- **Formato de título**: `RF-XXX: <resumen corto>`

### Plantilla estándar del issue (body)
Usar siempre esta estructura:

- `## Contexto`
- `## Descripción`
- `## Reglas`
- `## Criterios de aceptación`
- `## Referencia` (ej: ``docs/acueducto-veredal-requerimientos.md`` + RF)

### Reglas de formateo (crítico)
- **Nunca** construir el body con strings que contengan `\n` literal.
- Para `gh issue create/edit`, pasar el body con **HEREDOC**:

```bash
gh issue edit <NUM> -R <OWNER/REPO> --body "$(cat <<'EOF'
## Contexto
...

## Descripción
...
EOF
)"
```

### Autenticación / permisos (gh)
- Si falla por scopes de projects, refrescar:
  - `gh auth refresh --hostname github.com -s read:project`
  - `gh auth refresh --hostname github.com -s project`

### Flujo recomendado
1. **Recolectar RFs**
   - Si vienen de `docs/acueducto-veredal-requerimientos.md`, extraer títulos y descripciones.
2. **Crear issues**
   - `gh issue create -R <repo> --title "<RF>" --body "<BODY_MULTILINE>"`.
3. **Agregar a Project #7**
   - `gh project item-add 7 --owner gustavosotocano --url "<issue_url>"`.
4. **Verificación**
   - Listar issues creados y confirmar URLs.
   - Si algún issue quedó con formato malo, corregir con `gh issue edit` usando HEREDOC.

### Labels sugeridos (si el usuario lo pide)
- `backlog`
- `facturacion`
- `backend`
- `frontend`

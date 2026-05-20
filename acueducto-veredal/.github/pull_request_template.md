## Summary
<!-- Qué se hizo (1–3 oraciones). Historia: RF-XXX / issue # -->

## Code review (antes de QA)
<!-- Revisor: marcar cuando el diff esté listo. QA no empieza hasta esta sección aprobada. -->

- [ ] `./scripts/ci.sh` verde (dev)
- [ ] Cambio acotado a la RF / sin scope creep innecesario
- [ ] Multitenant, reglas de negocio y tests revisados
- [ ] **Approve** en GitHub (revisor ≠ autor) — o `CODE REVIEW PASS (self)` si equipo de 1 persona

**Revisor:** @<!-- mencionar cuenta GitHub -->

## QA scope
<!-- Marca uno: API | Frontend | Both -->

- [ ] API
- [ ] Frontend
- [ ] Both

## Test plan (dev)
<!-- Qué probaste en desarrollo. Incluye comando si aplica: `./scripts/ci.sh backend|frontend|all` -->

- [ ] Ejecuté `./scripts/ci.sh` (o el scope correspondiente) y pasó en verde
- [ ] Caso principal de la RF
- [ ] Casos borde relevantes
- [ ] Regresión mínima del módulo afectado

## Pasos para QA
<!-- Pasos concretos que QA debe ejecutar (URLs, pantallas, headers, datos de prueba) -->

1.
2.

## Criterios de aceptación
<!-- Checklist verificable copiado/adaptado del issue RF -->

- [ ]
- [ ]

## Referencias
<!-- Issue RF/bug, endpoints, pantallas -->

- Historia / RF:
- Endpoints:
- Pantallas:

## Impacto funcional

## Checklist técnico
- [ ] Cambié documentación si aplica
- [ ] Revisé reglas de negocio impactadas (`docs/05-business-rules.md`)
- [ ] Consideré aislamiento multitenant
- [ ] Consideré trazabilidad/auditoría

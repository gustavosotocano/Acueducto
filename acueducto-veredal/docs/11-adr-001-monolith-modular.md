# ADR-001 - Hexagonal Monolito modular multitenant

## Estado
Aceptado

## Contexto
El sistema debe soportar operación para uno o varios acueductos veredales, con reglas de negocio centradas en el derecho de agua, y un equipo de desarrollo inicial pequeño.

## Decisión
Implementar el sistema como un monolito modular multitenant con:

- Angular en frontend
- Spring Boot en backend
- PostgreSQL en base de datos
- aislamiento lógico mediante `tenant_id`

## Razones
- menor complejidad inicial
- más fácil de desplegar y mantener
- suficiente para el volumen esperado del negocio
- permite crecer después hacia integraciones y módulos más avanzados

## Consecuencias
### Positivas
- desarrollo más rápido del MVP
- menor costo operativo
- trazabilidad más simple

### Negativas
- requiere disciplina fuerte en aislamiento por tenant
- si crece mucho, algunos módulos podrían necesitar separación futura

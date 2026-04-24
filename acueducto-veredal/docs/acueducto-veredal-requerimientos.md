# Requerimientos de la aplicación `acueducto-veredal`

## 1. Objetivo general

Construir un sistema backend para la gestión operativa y administrativa de un **acueducto veredal**, enfocado en el control de derechos de agua, propietarios, predios, facturación, cartera y pagos.

El sistema debe permitir mantener la trazabilidad histórica de la relación entre:

- propietarios (`owner`)
- predios (`property`)
- derechos de agua (`water_right`)

Además, debe soportar **multitenancy**, de forma que múltiples organizaciones o acueductos puedan operar sobre la misma aplicación de manera aislada.

---

## 2. Principios del dominio

### 2.1 Core del negocio

El **núcleo del negocio** es la entidad `water_right` (derecho de agua).

Todo lo relacionado con:

- facturación
- cartera
- pagos
- estado del servicio
- historial de transferencias

se debe modelar alrededor del `water_right`.

### 2.2 Relación entre owner y water_right

- Un `owner` puede tener **uno o muchos** `water_rights`.
- Un `water_right` en un momento dado pertenece a **un solo owner actual**.
- Un `water_right` puede **transferirse** a otro owner.
- Toda transferencia debe conservar **historial completo**.

### 2.3 Relación entre property y owner

- Un `property` tiene un **owner actual**.
- La propiedad del predio puede cambiar con el tiempo.
- Debe mantenerse **historial de propietarios del predio**.

### 2.4 Separación entre predio y derecho de agua

Aunque normalmente un `water_right` está asociado a un `property`, el historial de propiedad del predio y el historial del derecho de agua deben manejarse como procesos separados.

Esto permite escenarios como:

- cambio de propietario del predio sin cambiar el derecho de agua
- transferencia del derecho de agua manteniendo el mismo predio
- regularizaciones futuras donde la propiedad y el derecho no coincidan temporalmente

---

## 3. Requerimientos funcionales

## 3.1 Gestión de tenants

### RF-001. Soporte multitenant
La aplicación debe soportar múltiples tenants.

### RF-002. Aislamiento por tenant
Toda la información debe estar aislada por `tenant_id`.

### RF-003. Tenant obligatorio
Todas las tablas principales deben incluir el campo `tenant_id`.

### RF-004. Resolución del tenant en cada request
El backend debe resolver el tenant en cada request, por ejemplo mediante header HTTP como `X-Tenant-Id`.

---

## 3.2 Gestión de propietarios (`owner`)

### RF-010. Crear owner
El sistema debe permitir crear propietarios.

Datos mínimos sugeridos:
- nombre completo o razón social
- documento o identificación
- teléfono
- correo electrónico
- estado

### RF-011. Consultar owners
El sistema debe permitir listar propietarios del tenant actual.

### RF-012. Consultar owner por id
El sistema debe permitir obtener un propietario por su identificador.

### RF-013. Actualizar owner
El sistema debe permitir actualizar la información básica de un propietario.

### RF-014. Desactivar owner
El sistema debe permitir desactivar lógicamente un propietario sin eliminarlo físicamente.

### RF-015. Soft state para owner
La desactivación debe reflejarse con un estado como `ACTIVE` / `INACTIVE`.

### RF-016. Restricción de borrado físico
No debe permitirse borrado físico directo si el owner tiene relaciones históricas o activas.

---

## 3.3 Gestión de predios (`property`)

### RF-020. Crear property
El sistema debe permitir crear predios.

Datos mínimos sugeridos:
- código interno
- nombre o descripción
- dirección
- owner actual
- estado

### RF-021. Consultar properties
El sistema debe permitir listar predios del tenant.

### RF-022. Consultar property por id
El sistema debe permitir consultar un predio por id.

### RF-023. Actualizar property
El sistema debe permitir actualizar los datos básicos del predio.

### RF-024. Desactivar property
El sistema debe permitir desactivar lógicamente un predio.

### RF-025. Cambio de owner de property
El sistema debe permitir cambiar el owner actual del predio.

### RF-026. Historial de propietarios del predio
Cada cambio de owner de un predio debe registrarse en `property_owner_history`.

### RF-027. Cierre del historial anterior
Cuando cambie el owner de un predio, el registro histórico anterior debe cerrarse con fecha de fin.

### RF-028. Nuevo historial activo
Después del cambio, debe crearse un nuevo registro histórico activo.

---

## 3.4 Gestión de derechos de agua (`water_right`)

### RF-030. Crear water_right
El sistema debe permitir crear derechos de agua.

Datos mínimos sugeridos:
- código o número del derecho
- owner actual
- property asociado
- estado del servicio
- fechas de vigencia
- observaciones

### RF-031. Consultar water_rights
El sistema debe permitir listar derechos de agua del tenant.

### RF-032. Consultar water_right por id
El sistema debe permitir consultar un derecho de agua por id.

### RF-033. Actualizar water_right
El sistema debe permitir actualizar la información editable del derecho de agua.

### RF-034. Desactivar water_right
El sistema debe permitir desactivar lógicamente un derecho de agua.

### RF-035. Soft state para water_right
La desactivación lógica debe manejarse mediante un estado como `service_status = ACTIVE/INACTIVE`.

### RF-036. Fecha de finalización de titularidad
Para desactivar un derecho de agua debe registrarse una fecha de finalización (`ownershipEndDate` o equivalente).

### RF-037. Transferir water_right
El sistema debe permitir transferir un derecho de agua de un owner a otro.

### RF-038. Historial de owners del water_right
Toda transferencia debe registrarse en `water_right_owner_history`.

### RF-039. Conservación del id del water_right
La transferencia no debe crear un nuevo `water_right`; debe mantenerse el mismo registro principal y cambiar el owner actual.

### RF-040. Cierre del historial anterior de water_right
Al transferir un `water_right`, el historial anterior debe cerrarse con fecha de fin.

### RF-041. Apertura del nuevo historial de water_right
La transferencia debe generar un nuevo registro histórico activo para el nuevo owner.

---

## 3.5 Facturación

### RF-050. Facturación asociada al water_right
Toda factura debe estar asociada a un `water_right`.

### RF-051. Generación de facturas
El sistema debe permitir generar facturas periódicas para derechos de agua activos.

### RF-052. Consulta de facturas
El sistema debe permitir consultar facturas por:
- tenant
- water_right
- owner
- estado
- rango de fechas

### RF-053. Estados de factura
Las facturas deben manejar estados, por ejemplo:
- borrador
- emitida
- pagada
- vencida
- anulada

### RF-054. Historial de facturación
Debe conservarse el historial de emisión y cambios de estado.

---

## 3.6 Cartera

### RF-060. Gestión de cartera por water_right
La cartera debe calcularse y consultarse a nivel de `water_right`.

### RF-061. Saldo pendiente
El sistema debe permitir conocer el saldo pendiente por derecho de agua.

### RF-062. Cartera por owner
El sistema debe permitir consolidar cartera por propietario.

### RF-063. Cartera vencida
El sistema debe identificar obligaciones vencidas.

---

## 3.7 Pagos

### RF-070. Registro de pagos
El sistema debe permitir registrar pagos asociados a facturas y/o a `water_right`.

### RF-071. Aplicación de pagos
Los pagos deben afectar la cartera correspondiente.

### RF-072. Historial de pagos
Debe mantenerse historial completo de pagos.

### RF-073. Consulta de pagos
El sistema debe permitir consultar pagos por owner, water_right, factura y fechas.

---

## 3.8 Auditoría e historial

### RF-080. Historial de ownership del predio
Debe existir un historial completo de cambios de owner del predio.

### RF-081. Historial de ownership del derecho de agua
Debe existir un historial completo de cambios de owner del derecho de agua.

### RF-082. Fechas de vigencia
Los historiales deben manejar al menos:
- fecha inicio
- fecha fin
- indicador de registro activo

### RF-083. No pérdida de trazabilidad
Ningún cambio de owner debe sobrescribir la historia previa sin dejar evidencia.

---

## 4. Requerimientos no funcionales

## RNF-001. Arquitectura backend
El backend debe construirse con **Spring Boot**.

## RNF-002. Base de datos relacional
La persistencia debe implementarse sobre una base de datos relacional, preferiblemente **PostgreSQL**.

## RNF-003. Migraciones versionadas
La estructura de base de datos debe manejarse con **Flyway**.

## RNF-004. Esquema inicial
Debe existir una migración inicial real como `V1__init.sql`.

## RNF-005. Seguridad del tenant
El tenant no debe mezclarse entre peticiones ni entre hilos de ejecución.

## RNF-006. Soft delete / soft state
Las entidades principales deben manejar desactivación lógica y no borrado físico directo.

## RNF-007. Trazabilidad
Las operaciones importantes del dominio deben quedar auditables.

## RNF-008. Diseño preparado para crecimiento
La solución debe dejar preparado el camino para módulos futuros como:
- lectura de medidores
- tarifas
- convenios de pago
- suspensiones y reconexiones
- reportes

## RNF-009. API REST clara
Los endpoints REST deben seguir convenciones coherentes y predecibles.

## RNF-010. Validación de datos
La API debe validar entrada obligatoria, consistencia del tenant y referencias entre entidades.

## RNF-011. Pruebas automatizadas
La aplicación debe contar con pruebas base de integración.

## RNF-012. Tests con entorno realista
Las pruebas de persistencia e integración deben apoyarse en **Testcontainers + PostgreSQL**.

---

## 5. Reglas de negocio importantes

### RB-001. El `water_right` es el eje transaccional
Facturación, cartera y pagos deben referenciar el `water_right` como eje principal.

### RB-002. Un owner puede tener muchos derechos
No se debe asumir relación uno a uno entre owner y water_right.

### RB-003. Historial obligatorio de transferencias
Cada transferencia de `water_right` debe dejar historial.

### RB-004. Historial obligatorio de propiedad del predio
Cada cambio de dueño del predio debe dejar historial.

### RB-005. Update normal no equivale a transferencia
Actualizar un `water_right` no debe usarse para cambiar owner. Para eso debe existir un caso de uso específico de transferencia.

### RB-006. Update normal de property no equivale a cambio de owner
Actualizar datos del predio no debe reemplazar el caso de uso específico de cambio de propietario.

### RB-007. Aislamiento por tenant
No puede existir consulta, update o transferencia cruzando datos de distintos tenants.

### RB-008. Desactivación lógica
La eliminación funcional de entidades debe manejarse por estado y no por borrado físico.

---

## 6. Modelo conceptual inicial

## Entidades principales

### `owner`
Representa una persona natural o jurídica titular.

Campos base sugeridos:
- id
- tenant_id
- full_name
- document_number
- phone
- email
- status
- created_at
- updated_at

### `property`
Representa el predio asociado al servicio.

Campos base sugeridos:
- id
- tenant_id
- owner_id
- property_code
- name
- address
- status
- created_at
- updated_at

### `water_right`
Representa el derecho de agua.

Campos base sugeridos:
- id
- tenant_id
- owner_id
- property_id
- right_code
- service_status
- start_date
- end_date
- created_at
- updated_at

### `property_owner_history`
Historial de propietarios del predio.

Campos base sugeridos:
- id
- tenant_id
- property_id
- owner_id
- start_date
- end_date
- active
- created_at

### `water_right_owner_history`
Historial de propietarios del derecho de agua.

Campos base sugeridos:
- id
- tenant_id
- water_right_id
- owner_id
- start_date
- end_date
- active
- created_at

### Futuras entidades del dominio
- invoice
- payment
- receivable / portfolio entry
- tariff
- meter
- meter_reading
- service_suspension
- reconnection

---

## 7. Casos de uso iniciales del MVP

### CU-001. Crear propietario
### CU-002. Listar propietarios
### CU-003. Consultar propietario por id
### CU-004. Actualizar propietario
### CU-005. Desactivar propietario

### CU-010. Crear predio
### CU-011. Listar predios
### CU-012. Consultar predio por id
### CU-013. Actualizar predio
### CU-014. Desactivar predio
### CU-015. Cambiar propietario del predio
### CU-016. Consultar historial de propietarios del predio

### CU-020. Crear derecho de agua
### CU-021. Listar derechos de agua
### CU-022. Consultar derecho de agua por id
### CU-023. Actualizar derecho de agua
### CU-024. Desactivar derecho de agua
### CU-025. Transferir derecho de agua
### CU-026. Consultar historial de propietarios del derecho de agua

---

## 8. Recomendaciones técnicas de implementación

### 8.1 Multitenancy
Opciones sugeridas:
- `TenantContext` por request
- filtro HTTP para capturar `X-Tenant-Id`
- enforcement del tenant en servicios y repositorios
- evaluar filtros/interceptors a nivel JPA

### 8.2 Persistencia
- Spring Data JPA
- PostgreSQL
- Flyway
- índices compuestos por `tenant_id` y claves de negocio relevantes

### 8.3 Diseño de API
Ejemplos de endpoints base:

- `POST /api/v1/owners`
- `GET /api/v1/owners`
- `GET /api/v1/owners/{id}`
- `PUT /api/v1/owners/{id}`
- `POST /api/v1/owners/{id}/deactivate`

- `POST /api/v1/properties`
- `GET /api/v1/properties`
- `GET /api/v1/properties/{id}`
- `PUT /api/v1/properties/{id}`
- `POST /api/v1/properties/{id}/deactivate`
- `POST /api/v1/properties/{id}/owner-changes`
- `GET /api/v1/properties/{id}/owner-history`

- `POST /api/v1/water-rights`
- `GET /api/v1/water-rights`
- `GET /api/v1/water-rights/{id}`
- `PUT /api/v1/water-rights/{id}`
- `POST /api/v1/water-rights/{id}/deactivate`
- `POST /api/v1/water-rights/{id}/transfers`
- `GET /api/v1/water-rights/{id}/owner-history`

---

## 9. Pendientes funcionales para siguientes iteraciones

1. Facturación real
2. Gestión de cartera
3. Registro y aplicación de pagos
4. Tarifas
5. Lectura de medidores
6. Suspensión y reconexión del servicio
7. Reportes administrativos
8. Seguridad y autenticación
9. Auditoría detallada de cambios
10. Tests de integración con Testcontainers

---

## 10. Criterios de aceptación iniciales

### CA-001
Toda entidad principal debe incluir `tenant_id`.

### CA-002
No deben existir operaciones que lean o modifiquen datos de otro tenant.

### CA-003
Debe poder crearse un owner, property y water_right.

### CA-004
Debe poder actualizarse y desactivarse owner, property y water_right.

### CA-005
Debe poder transferirse un `water_right` entre owners sin perder historial.

### CA-006
Debe poder cambiarse el owner de un `property` sin perder historial.

### CA-007
El historial del predio y el historial del derecho de agua deben consultarse por endpoint.

### CA-008
La base de datos debe poder inicializarse desde migraciones Flyway.

---

## 11. Resumen ejecutivo

La aplicación `acueducto-veredal` debe construirse como un backend multitenant donde el eje del negocio es el **derecho de agua (`water_right`)**. El sistema debe permitir administrar propietarios, predios y derechos de agua, manteniendo historial de transferencias y cambios de propiedad, y dejando lista la base para soportar facturación, cartera y pagos sobre el `water_right`.

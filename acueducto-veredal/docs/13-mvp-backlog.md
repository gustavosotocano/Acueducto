# MVP Backlog

## Epic 1. Gestión de tenants
- Crear acueducto veredal
- Configurar datos básicos del tenant
- Aislar operación por tenant
- Administrar usuarios operativos por tenant
- **RF-067** — Crear tenant con presidente inicial en el mismo formulario (backend + UI)
  - [stories/RF-067-tenant-create-with-president.md](stories/RF-067-tenant-create-with-president.md)
- **RF-068** — Usuarios por alcance: admin plataforma (selector tenant) y presidente (solo su tenant, UI + API)
  - [stories/RF-068-user-management-by-tenant-scope.md](stories/RF-068-user-management-by-tenant-scope.md)

## Epic 2. Gestión de dueños
- Crear dueño de derecho de agua
- Editar dueño de derecho de agua
- Consultar listado de dueños
- Consultar todos los derechos asociados a una persona
- Desactivar dueño

## Epic 3. Gestión de derechos de agua
- Configurar cantidad total de derechos
- Registrar compra o asignación de derecho
- Asociar derechos a dueño
- Transferir derecho entre propietarios conservando el id
- Mantener historial de propietarios
- Mantener historial de inmueble o dirección asociada
- Consultar derechos disponibles y asignados

## Epic 4. Tarifas y conceptos
- Crear tarifa de mensualidad
- Crear tarifa de compra de derecho
- Mantener histórico de vigencias

## Epic 5. Facturación
- Generar facturas mensuales por derecho de agua
- Consultar factura por usuario y por derecho
- Exportar factura a PDF
- Marcar factura como anulada con trazabilidad

## Epic 6. Pagos y recibos
- Registrar pago total
- Registrar pago parcial
- Aplicar pago a facturas vencidas
- Generar recibo de caja PDF

## Epic 7. Cartera
- Consultar saldo por derecho de agua
- Consultar saldo consolidado por persona
- Consultar meses vencidos
- Generar reporte de cartera

## Epic 8. Gastos
- Registrar gasto
- Editar gasto
- Anular gasto con trazabilidad
- Consultar gastos por periodo

## Epic 9. Cierre mensual
- Calcular ingresos del mes
- Calcular gastos del mes
- Calcular balance neto
- Generar reporte de cierre mensual

## Epic 10. Seguridad
- Login con correo y contraseña
- OTP por correo
- Gestión de roles
- Auditoría de acciones críticas
- **RF-067** / **RF-068** — Modelo plataforma (`ADMIN` sin tenant) vs acueducto (`PRESIDENT` + operativos `TREASURY`); ver Epic 1

## Epic 11. Arquitectura backend (habilitadores)
- **RNF-013** — Fronteras entre módulos: piloto `payments` ↔ `invoices` — [#29](https://github.com/gustavosotocano/Acueducto/issues/29)
  - [stories/RNF-013-module-boundaries-and-ports.md](stories/RNF-013-module-boundaries-and-ports.md)
- **RNF-014** — Puertos `invoices` ↔ `waterrights` / `tariffs` — [#30](https://github.com/gustavosotocano/Acueducto/issues/30)
  - [stories/RNF-014-invoices-waterrights-tariffs-ports.md](stories/RNF-014-invoices-waterrights-tariffs-ports.md)
- **RNF-015** — Puertos `portfolio` ↔ `invoices` / `interests` — [#31](https://github.com/gustavosotocano/Acueducto/issues/31)
  - [stories/RNF-015-portfolio-invoices-interests-ports.md](stories/RNF-015-portfolio-invoices-interests-ports.md)
- **RNF-016** — Puertos `waterrights` ↔ `owners` / `properties` — [#32](https://github.com/gustavosotocano/Acueducto/issues/32)
  - [stories/RNF-016-waterrights-owners-properties-ports.md](stories/RNF-016-waterrights-owners-properties-ports.md)
- **RNF-017** — Virtual threads + `ScopedValue` para `TenantContext` — [#33](https://github.com/gustavosotocano/Acueducto/issues/33)
  - [stories/RNF-017-virtual-threads-and-tenant-context.md](stories/RNF-017-virtual-threads-and-tenant-context.md)

# Reglas de negocio

## RB-01
Una persona puede tener uno o varios derechos de agua.

## RB-02
Un derecho de agua pertenece a un único titular activo dentro de un acueducto en un momento dado.

## RB-03
Los procesos de facturación, cartera, cobro y estado de cuenta giran alrededor del derecho de agua como unidad principal de negocio.

## RB-04
La factura mensual se genera por cada derecho de agua activo usando la tarifa vigente.

## RB-05
La cartera debe calcularse por derecho de agua y por factura, aunque el sistema debe permitir consulta consolidada por persona.

## RB-06
Una persona debe poder consultar toda su información consolidada, incluyendo todos sus derechos, facturas, pagos, saldos y cartera asociados.

## RB-07
Si existen saldos pendientes en un derecho de agua, estos deben reflejarse en las siguientes facturas del mismo derecho según las reglas definidas.

## RB-08
Un pago parcial debe disminuir la deuda sin marcar la factura como pagada hasta completar el saldo.

## RB-09
La compra, venta o transferencia de un derecho de agua debe quedar registrada como movimiento histórico, conservando el mismo identificador del derecho de agua.

## RB-10
Un derecho de agua puede ser transferido a otro titular sin cambiar su identificador funcional, actualizando únicamente el propietario vigente y preservando el historial completo de propietarios anteriores.

## RB-11
La vivienda o dirección de servicio asociada a un derecho de agua también puede cambiar en el tiempo, y el sistema debe mantener historial de esas variaciones para trazabilidad.

## RB-12
Las transferencias de derechos deben registrar como mínimo: fecha, titular anterior, titular nuevo, motivo u observación, usuario que registró el cambio y vigencia del cambio.

## RB-13
Solo usuarios con rol autorizado pueden modificar tarifas, transferir derechos o anular comprobantes.

## RB-14
Los gastos no pueden eliminarse físicamente; deben anularse o reversarse para conservar trazabilidad.

## RB-15
El cierre mensual no bloquea la operación, pero sí genera una foto histórica del periodo.

## RB-16
La aplicación debe soportar múltiples acueductos veredales aislando datos, configuración, facturación, cartera, usuarios operativos y reportes por tenant.

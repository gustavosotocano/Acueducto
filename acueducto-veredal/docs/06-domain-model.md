# Modelo de dominio inicial

Campos sugeridos:
- paymentId
- tenantId
- ownerId
- paymentDate
- amount
- paymentMethod
- reference
- notes
- receivedBy

### PaymentApplication
Aplicación de pago a factura.

Campos sugeridos:
- paymentApplicationId
- tenantId
- paymentId
- invoiceId
- waterRightId
- appliedAmount

### CashReceipt
Recibo de caja.

Campos sugeridos:
- cashReceiptId
- tenantId
- receiptNumber
- paymentId
- issueDate
- totalReceived

### Expense
Gasto.

Campos sugeridos:
- expenseId
- tenantId
- expenseDate
- category
- description
- supplierName
- amount
- attachmentUrl
- status

### MonthlyClosing
Cierre mensual.

Campos sugeridos:
- closingId
- tenantId
- year
- month
- invoicedTotal
- collectedTotal
- expenseTotal
- outstandingPortfolio
- netBalance
- generatedAt

### User
Usuario interno del sistema.

Campos sugeridos:
- userId
- tenantId
- fullName
- email
- phone
- passwordHash
- role
- status
- twoFactorMethod

## Relaciones clave

- Un `Tenant` tiene muchos `Owner`.
- Un `Tenant` tiene muchos `WaterRight`.
- Un `Owner` puede tener muchos `WaterRight` a lo largo del tiempo.
- Un `WaterRight` tiene un único propietario vigente, pero múltiples registros en `WaterRightOwnershipHistory`.
- Un `WaterRight` puede tener cambios de vivienda o dirección, almacenados en `WaterRightPropertyHistory`.
- Un `WaterRight` tiene muchas `Invoice`.
- Un `Owner` puede consultarse de forma consolidada, pero la deuda y facturación viven a nivel `WaterRight`.
- Todas las entidades operativas deben estar aisladas por `tenantId`.

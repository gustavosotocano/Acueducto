package com.acueducto.veredal.payments.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findAllByTenantIdOrderByPaymentDateDesc(String tenantId);

    List<PaymentEntity> findAllByTenantIdAndInvoiceIdOrderByPaymentDateDesc(String tenantId, UUID invoiceId);

    List<PaymentEntity> findAllByTenantIdAndOwnerIdOrderByPaymentDateDesc(String tenantId, UUID ownerId);

    List<PaymentEntity> findAllByTenantIdAndWaterRightIdOrderByPaymentDateDesc(String tenantId, UUID waterRightId);

    List<PaymentEntity> findAllByTenantIdAndPaymentDateBetweenOrderByPaymentDateDesc(
            String tenantId,
            LocalDate from,
            LocalDate to
    );

    @Query("""
            select coalesce(sum(p.amount), 0)
            from PaymentEntity p
            where p.tenantId = :tenantId and p.invoiceId = :invoiceId
            """)
    BigDecimal sumAmountByTenantIdAndInvoiceId(@Param("tenantId") String tenantId, @Param("invoiceId") UUID invoiceId);
}


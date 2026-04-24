package com.acueducto.veredal.portfolio.infrastructure.persistence.jpa;

import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PortfolioRepository extends Repository<InvoiceEntity, UUID> {

    interface WaterRightBalanceRow {
        UUID getWaterRightId();
        UUID getOwnerId();
        java.math.BigDecimal getBilledAmount();
        java.math.BigDecimal getPaidAmount();
        java.math.BigDecimal getOutstandingAmount();
    }

    interface OwnerBalanceRow {
        UUID getOwnerId();
        java.math.BigDecimal getBilledAmount();
        java.math.BigDecimal getPaidAmount();
        java.math.BigDecimal getOutstandingAmount();
    }

    interface OverdueBucketRow {
        String getBucket();
        int getInvoiceCount();
        java.math.BigDecimal getOutstandingAmount();
        int getBucketSort();
    }

    interface OverdueInvoiceRow {
        java.util.UUID getInvoiceId();
        java.time.LocalDate getDueDate();
        java.math.BigDecimal getOutstandingAmount();
    }

    @Query(value = """
            select
              i.water_right_id as waterRightId,
              i.owner_id as ownerId,
              sum(i.total_amount) as billedAmount,
              sum(coalesce(p.paid_amount, 0)) as paidAmount,
              sum(i.total_amount - coalesce(p.paid_amount, 0)) as outstandingAmount
            from invoice i
            left join (
              select tenant_id, invoice_id, sum(amount) as paid_amount
              from payment
              where tenant_id = :tenantId
              group by tenant_id, invoice_id
            ) p on p.tenant_id = i.tenant_id and p.invoice_id = i.id
            where i.tenant_id = :tenantId
              and i.status <> 'CANCELLED'
            group by i.water_right_id, i.owner_id
            having sum(i.total_amount - coalesce(p.paid_amount, 0)) > 0
            order by outstandingAmount desc
            """, nativeQuery = true)
    List<WaterRightBalanceRow> findOutstandingByWaterRight(@Param("tenantId") String tenantId);

    @Query(value = """
            select
              i.owner_id as ownerId,
              sum(i.total_amount) as billedAmount,
              sum(coalesce(p.paid_amount, 0)) as paidAmount,
              sum(i.total_amount - coalesce(p.paid_amount, 0)) as outstandingAmount
            from invoice i
            left join (
              select tenant_id, invoice_id, sum(amount) as paid_amount
              from payment
              where tenant_id = :tenantId
              group by tenant_id, invoice_id
            ) p on p.tenant_id = i.tenant_id and p.invoice_id = i.id
            where i.tenant_id = :tenantId
              and i.status <> 'CANCELLED'
            group by i.owner_id
            having sum(i.total_amount - coalesce(p.paid_amount, 0)) > 0
            order by outstandingAmount desc
            """, nativeQuery = true)
    List<OwnerBalanceRow> findOutstandingByOwner(@Param("tenantId") String tenantId);

    @Query(value = """
            with paid_by_invoice as (
              select tenant_id, invoice_id, sum(amount) as paid_amount
              from payment
              where tenant_id = :tenantId
              group by tenant_id, invoice_id
            ),
            invoice_outstanding as (
              select
                i.id as invoice_id,
                i.due_date,
                (i.total_amount - coalesce(p.paid_amount, 0)) as outstanding_amount
              from invoice i
              left join paid_by_invoice p on p.tenant_id = i.tenant_id and p.invoice_id = i.id
              where i.tenant_id = :tenantId
                and i.status <> 'CANCELLED'
                and (i.total_amount - coalesce(p.paid_amount, 0)) > 0
                and i.due_date < current_date
            )
            select
              case
                when (current_date - due_date) between 1 and 30 then '0-30'
                when (current_date - due_date) between 31 and 60 then '31-60'
                when (current_date - due_date) between 61 and 90 then '61-90'
                else '>90'
              end as bucket,
              count(*) as invoiceCount,
              sum(outstanding_amount) as outstandingAmount,
              case
                when (current_date - due_date) between 1 and 30 then 1
                when (current_date - due_date) between 31 and 60 then 2
                when (current_date - due_date) between 61 and 90 then 3
                else 4
              end as bucketSort
            from invoice_outstanding
            group by bucket, bucketSort
            order by bucketSort asc
            """, nativeQuery = true)
    List<OverdueBucketRow> findOverdueSummary(@Param("tenantId") String tenantId);

    @Query(value = """
            with paid_by_invoice as (
              select tenant_id, invoice_id, sum(amount) as paid_amount
              from payment
              where tenant_id = :tenantId
              group by tenant_id, invoice_id
            )
            select
              i.id as invoiceId,
              i.due_date as dueDate,
              (i.total_amount - coalesce(p.paid_amount, 0)) as outstandingAmount
            from invoice i
            left join paid_by_invoice p on p.tenant_id = i.tenant_id and p.invoice_id = i.id
            where i.tenant_id = :tenantId
              and i.status <> 'CANCELLED'
              and (i.total_amount - coalesce(p.paid_amount, 0)) > 0
              and i.due_date < current_date
            """, nativeQuery = true)
    List<OverdueInvoiceRow> findOverdueInvoices(@Param("tenantId") String tenantId);
}


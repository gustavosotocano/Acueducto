package com.acueducto.veredal.portfolio.application;

import com.acueducto.veredal.interests.application.InterestPolicyService;
import com.acueducto.veredal.portfolio.infrastructure.persistence.jpa.PortfolioRepository;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final InterestPolicyService interestPolicyService;

    public PortfolioService(PortfolioRepository portfolioRepository, InterestPolicyService interestPolicyService) {
        this.portfolioRepository = portfolioRepository;
        this.interestPolicyService = interestPolicyService;
    }

    public List<PortfolioRepository.WaterRightBalanceRow> outstandingByWaterRight() {
        return portfolioRepository.findOutstandingByWaterRight(TenantContext.getRequiredTenantId());
    }

    public List<PortfolioRepository.OwnerBalanceRow> outstandingByOwner() {
        return portfolioRepository.findOutstandingByOwner(TenantContext.getRequiredTenantId());
    }

    public List<PortfolioRepository.OverdueBucketRow> overdueSummary() {
        return portfolioRepository.findOverdueSummary(TenantContext.getRequiredTenantId());
    }

    public List<OverdueBucketWithInterestRow> overdueSummaryWithInterest() {
        String tenantId = TenantContext.getRequiredTenantId();
        boolean interestEnabled = interestPolicyService.isEnabled();
        List<PortfolioRepository.OverdueInvoiceRow> overdue = portfolioRepository.findOverdueInvoices(tenantId);

        Map<String, OverdueBucketWithInterestRow> buckets = new LinkedHashMap<>();
        buckets.put("0-30", new OverdueBucketWithInterestRow("0-30"));
        buckets.put("31-60", new OverdueBucketWithInterestRow("31-60"));
        buckets.put("61-90", new OverdueBucketWithInterestRow("61-90"));
        buckets.put(">90", new OverdueBucketWithInterestRow(">90"));

        LocalDate today = LocalDate.now();
        for (PortfolioRepository.OverdueInvoiceRow row : overdue) {
            int days = (int) ChronoUnit.DAYS.between(row.getDueDate(), today);
            String bucket = bucketForDays(days);
            OverdueBucketWithInterestRow acc = buckets.get(bucket);

            BigDecimal outstanding = row.getOutstandingAmount();
            acc.invoiceCount++;
            acc.outstandingAmount = acc.outstandingAmount.add(outstanding);

            if (interestEnabled) {
                BigDecimal ratePercent = interestPolicyService.resolveRatePercent(days);
                BigDecimal interest = outstanding.multiply(ratePercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                acc.interestAmount = acc.interestAmount.add(interest);
            }
        }

        return List.of(buckets.get("0-30"), buckets.get("31-60"), buckets.get("61-90"), buckets.get(">90"));
    }

    private String bucketForDays(int daysOverdue) {
        if (daysOverdue <= 30) return "0-30";
        if (daysOverdue <= 60) return "31-60";
        if (daysOverdue <= 90) return "61-90";
        return ">90";
    }

    public static class OverdueBucketWithInterestRow {
        public final String bucket;
        public int invoiceCount = 0;
        public BigDecimal outstandingAmount = BigDecimal.ZERO;
        public BigDecimal interestAmount = BigDecimal.ZERO;

        public OverdueBucketWithInterestRow(String bucket) {
            this.bucket = bucket;
        }

        public BigDecimal totalWithInterest() {
            return outstandingAmount.add(interestAmount);
        }
    }
}


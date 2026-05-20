package com.acueducto.veredal.invoices.infrastructure.portfolio;

import com.acueducto.veredal.portfolio.application.port.OwnerBalanceSnapshot;
import com.acueducto.veredal.portfolio.application.port.OverdueBucketSnapshot;
import com.acueducto.veredal.portfolio.application.port.OverdueInvoiceSnapshot;
import com.acueducto.veredal.portfolio.application.port.PortfolioReadPort;
import com.acueducto.veredal.portfolio.application.port.WaterRightBalanceSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioReadJpaAdapter implements PortfolioReadPort {

    private final PortfolioInvoiceReadRepository portfolioInvoiceReadRepository;

    public PortfolioReadJpaAdapter(PortfolioInvoiceReadRepository portfolioInvoiceReadRepository) {
        this.portfolioInvoiceReadRepository = portfolioInvoiceReadRepository;
    }

    @Override
    public List<WaterRightBalanceSnapshot> findOutstandingByWaterRight(String tenantId) {
        return portfolioInvoiceReadRepository.findOutstandingByWaterRight(tenantId).stream()
                .map(row -> new WaterRightBalanceSnapshot(
                        row.getWaterRightId(),
                        row.getOwnerId(),
                        row.getBilledAmount(),
                        row.getPaidAmount(),
                        row.getOutstandingAmount()
                ))
                .toList();
    }

    @Override
    public List<OwnerBalanceSnapshot> findOutstandingByOwner(String tenantId) {
        return portfolioInvoiceReadRepository.findOutstandingByOwner(tenantId).stream()
                .map(row -> new OwnerBalanceSnapshot(
                        row.getOwnerId(),
                        row.getBilledAmount(),
                        row.getPaidAmount(),
                        row.getOutstandingAmount()
                ))
                .toList();
    }

    @Override
    public List<OverdueBucketSnapshot> findOverdueSummary(String tenantId) {
        return portfolioInvoiceReadRepository.findOverdueSummary(tenantId).stream()
                .map(row -> new OverdueBucketSnapshot(
                        row.getBucket(),
                        row.getInvoiceCount(),
                        row.getOutstandingAmount()
                ))
                .toList();
    }

    @Override
    public List<OverdueInvoiceSnapshot> findOverdueInvoices(String tenantId) {
        return portfolioInvoiceReadRepository.findOverdueInvoices(tenantId).stream()
                .map(row -> new OverdueInvoiceSnapshot(
                        row.getInvoiceId(),
                        row.getDueDate(),
                        row.getOutstandingAmount()
                ))
                .toList();
    }
}

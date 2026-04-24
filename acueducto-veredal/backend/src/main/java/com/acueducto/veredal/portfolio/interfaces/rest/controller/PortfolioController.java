package com.acueducto.veredal.portfolio.interfaces.rest.controller;

import com.acueducto.veredal.portfolio.application.PortfolioService;
import com.acueducto.veredal.portfolio.infrastructure.persistence.jpa.PortfolioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/water-rights")
    public List<WaterRightBalanceResponse> outstandingByWaterRight() {
        return portfolioService.outstandingByWaterRight().stream().map(this::toWaterRightResponse).toList();
    }

    @GetMapping("/owners")
    public List<OwnerBalanceResponse> outstandingByOwner() {
        return portfolioService.outstandingByOwner().stream().map(this::toOwnerResponse).toList();
    }

    @GetMapping("/overdue")
    public List<OverdueBucketResponse> overdueSummary() {
        return portfolioService.overdueSummary().stream().map(this::toOverdueResponse).toList();
    }

    @GetMapping("/overdue-with-interest")
    public List<OverdueBucketWithInterestResponse> overdueSummaryWithInterest() {
        return portfolioService.overdueSummaryWithInterest().stream().map(this::toOverdueWithInterestResponse).toList();
    }

    private WaterRightBalanceResponse toWaterRightResponse(PortfolioRepository.WaterRightBalanceRow row) {
        return new WaterRightBalanceResponse(
                row.getWaterRightId(),
                row.getOwnerId(),
                row.getBilledAmount(),
                row.getPaidAmount(),
                row.getOutstandingAmount()
        );
    }

    private OwnerBalanceResponse toOwnerResponse(PortfolioRepository.OwnerBalanceRow row) {
        return new OwnerBalanceResponse(
                row.getOwnerId(),
                row.getBilledAmount(),
                row.getPaidAmount(),
                row.getOutstandingAmount()
        );
    }

    private OverdueBucketResponse toOverdueResponse(PortfolioRepository.OverdueBucketRow row) {
        return new OverdueBucketResponse(
                row.getBucket(),
                row.getInvoiceCount(),
                row.getOutstandingAmount()
        );
    }

    private OverdueBucketWithInterestResponse toOverdueWithInterestResponse(com.acueducto.veredal.portfolio.application.PortfolioService.OverdueBucketWithInterestRow row) {
        return new OverdueBucketWithInterestResponse(
                row.bucket,
                row.invoiceCount,
                row.outstandingAmount,
                row.interestAmount,
                row.totalWithInterest()
        );
    }
}


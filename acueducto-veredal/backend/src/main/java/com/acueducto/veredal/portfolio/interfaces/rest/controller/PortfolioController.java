package com.acueducto.veredal.portfolio.interfaces.rest.controller;

import com.acueducto.veredal.portfolio.application.PortfolioService;
import com.acueducto.veredal.portfolio.application.port.OwnerBalanceSnapshot;
import com.acueducto.veredal.portfolio.application.port.OverdueBucketSnapshot;
import com.acueducto.veredal.portfolio.application.port.WaterRightBalanceSnapshot;
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

    private WaterRightBalanceResponse toWaterRightResponse(WaterRightBalanceSnapshot row) {
        return new WaterRightBalanceResponse(
                row.waterRightId(),
                row.ownerId(),
                row.billedAmount(),
                row.paidAmount(),
                row.outstandingAmount()
        );
    }

    private OwnerBalanceResponse toOwnerResponse(OwnerBalanceSnapshot row) {
        return new OwnerBalanceResponse(
                row.ownerId(),
                row.billedAmount(),
                row.paidAmount(),
                row.outstandingAmount()
        );
    }

    private OverdueBucketResponse toOverdueResponse(OverdueBucketSnapshot row) {
        return new OverdueBucketResponse(
                row.bucket(),
                row.invoiceCount(),
                row.outstandingAmount()
        );
    }

    private OverdueBucketWithInterestResponse toOverdueWithInterestResponse(PortfolioService.OverdueBucketWithInterestRow row) {
        return new OverdueBucketWithInterestResponse(
                row.bucket,
                row.invoiceCount,
                row.outstandingAmount,
                row.interestAmount,
                row.totalWithInterest()
        );
    }
}

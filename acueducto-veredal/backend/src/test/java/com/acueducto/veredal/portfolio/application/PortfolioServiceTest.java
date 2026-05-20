package com.acueducto.veredal.portfolio.application;

import com.acueducto.veredal.interests.application.InterestPolicyService;
import com.acueducto.veredal.portfolio.application.port.OverdueInvoiceSnapshot;
import com.acueducto.veredal.portfolio.application.port.PortfolioReadPort;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final String TENANT = "tenant_portfolio";

    @Mock
    private PortfolioReadPort portfolioReadPort;

    @Mock
    private InterestPolicyService interestPolicyService;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void overdueSummaryWithInterest_aggregatesBucketAndInterest() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.now().minusDays(10);

        TenantContext.runWithTenant(TENANT, () -> {
            when(portfolioReadPort.findOverdueInvoices(TENANT)).thenReturn(List.of(
                    new OverdueInvoiceSnapshot(invoiceId, dueDate, new BigDecimal("1000.00"))
            ));
            when(interestPolicyService.isEnabled()).thenReturn(true);
            when(interestPolicyService.resolveRatePercent(10)).thenReturn(new BigDecimal("5.00"));

            List<PortfolioService.OverdueBucketWithInterestRow> rows = portfolioService.overdueSummaryWithInterest();

            PortfolioService.OverdueBucketWithInterestRow bucket030 = rows.stream()
                    .filter(r -> "0-30".equals(r.bucket))
                    .findFirst()
                    .orElseThrow();
            assertThat(bucket030.invoiceCount).isEqualTo(1);
            assertThat(bucket030.outstandingAmount).isEqualByComparingTo("1000.00");
            assertThat(bucket030.interestAmount).isEqualByComparingTo("50.00");
            assertThat(bucket030.totalWithInterest()).isEqualByComparingTo("1050.00");
        });
    }

    @Test
    void overdueSummaryWithInterest_skipsInterestWhenDisabled() throws Exception {
        LocalDate dueDate = LocalDate.now().minusDays(45);

        TenantContext.runWithTenant(TENANT, () -> {
            when(portfolioReadPort.findOverdueInvoices(TENANT)).thenReturn(List.of(
                    new OverdueInvoiceSnapshot(UUID.randomUUID(), dueDate, new BigDecimal("2000.00"))
            ));
            when(interestPolicyService.isEnabled()).thenReturn(false);

            List<PortfolioService.OverdueBucketWithInterestRow> rows = portfolioService.overdueSummaryWithInterest();

            PortfolioService.OverdueBucketWithInterestRow bucket3160 = rows.stream()
                    .filter(r -> "31-60".equals(r.bucket))
                    .findFirst()
                    .orElseThrow();
            assertThat(bucket3160.interestAmount).isEqualByComparingTo("0");
            assertThat(bucket3160.totalWithInterest()).isEqualByComparingTo("2000.00");
        });
    }
}

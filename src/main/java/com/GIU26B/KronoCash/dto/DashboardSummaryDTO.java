package com.GIU26B.KronoCash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Resumen agregado que consume la vista /dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalSavedInGoals = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal monthlyIncome = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal monthlyExpense = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal monthlyNet = BigDecimal.ZERO;
    @Builder.Default
    private long transactionCount = 0;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    @Builder.Default
    private List<GoalProgress> goals = new ArrayList<>();

    /** Avance de una meta de ahorro, con porcentaje ya calculado. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProgress {
        private Long id;
        private String title;
        private BigDecimal targetAmount;
        private BigDecimal currentAmount;
        private BigDecimal remainingAmount;
        private BigDecimal progressPercent;
    }
}

package com.GIU26B.KronoCash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Resultado de la proyección, con desglose mes a mes para la tabla accesible. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultDTO {

    private BigDecimal monthlyIncome;
    private BigDecimal savingsPercentage;
    private BigDecimal antExpensesCut;
    private Integer monthsHorizon;

    /** Ahorro base mensual = ingreso mensual × porcentaje de ahorro. */
    private BigDecimal baseMonthlySavings;
    /** Ahorro extra mensual = recorte de gastos hormiga. */
    private BigDecimal extraMonthlySavings;
    private BigDecimal totalBaseSavings;
    private BigDecimal totalExtraSavings;
    private BigDecimal projectedTotal;

    @Builder.Default
    private List<MonthlyProjection> breakdown = new ArrayList<>();

    /** Fila del desglose mensual; los importes son acumulados. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyProjection {
        private int month;
        private BigDecimal accumulatedBaseSavings;
        private BigDecimal accumulatedExtraSavings;
        private BigDecimal accumulatedTotal;
    }
}

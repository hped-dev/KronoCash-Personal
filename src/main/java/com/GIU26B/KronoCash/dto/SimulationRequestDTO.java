package com.GIU26B.KronoCash.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Parámetros del simulador; los mismos sirven para calcular y para guardar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequestDTO {

    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    @Builder.Default
    private String scenarioName = "Escenario";

    @NotNull(message = "Indique el ingreso mensual.")
    @DecimalMin(value = "0", message = "El ingreso no puede ser negativo.")
    @DecimalMax(value = "25000.00", message = "El ingreso mensual máximo es 25,000 MXN.")
    @Digits(integer = 10, fraction = 2, message = "Use hasta 10 enteros y 2 decimales.")
    @Builder.Default
    private BigDecimal monthlyIncome = BigDecimal.ZERO;

    @NotNull(message = "Indique el porcentaje de ahorro.")
    @DecimalMin(value = "0", message = "El porcentaje mínimo es 0.")
    @DecimalMax(value = "100", message = "El porcentaje máximo es 100.")
    @Digits(integer = 3, fraction = 2, message = "Use hasta 3 enteros y 2 decimales.")
    @Builder.Default
    private BigDecimal savingsPercentage = BigDecimal.ZERO;

    @NotNull(message = "Indique el recorte de gastos hormiga.")
    @DecimalMin(value = "0", message = "El recorte no puede ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "Use hasta 10 enteros y 2 decimales.")
    @Builder.Default
    private BigDecimal antExpensesCut = BigDecimal.ZERO;

    @NotNull(message = "Indique el horizonte en meses.")
    @Min(value = 1, message = "El horizonte mínimo es 1 mes.")
    @Max(value = 120, message = "El horizonte máximo es 120 meses.")
    @Builder.Default
    private Integer monthsHorizon = 12;

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName == null || scenarioName.isBlank() ? "Escenario" : scenarioName.trim();
    }
}

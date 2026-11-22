package com.GIU26B.KronoCash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formulario de alta de metas de ahorro. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalFormDTO {

    @NotBlank(message = "El título de la meta es obligatorio.")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
    private String title;

    @NotNull(message = "Indique el importe objetivo.")
    @DecimalMin(value = "0.01", message = "El objetivo debe ser mayor que cero.")
    @Digits(integer = 10, fraction = 2, message = "Use hasta 10 enteros y 2 decimales.")
    private BigDecimal targetAmount;

    private LocalDate deadline;

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }
}

package com.GIU26B.KronoCash.dto;

import com.GIU26B.KronoCash.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formulario de alta de ingresos y egresos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFormDTO {

    @NotNull(message = "Seleccione el tipo de movimiento.")
    private TransactionType type;

    @NotBlank(message = "La categoría es obligatoria.")
    @Size(max = 50, message = "La categoría no puede superar los 50 caracteres.")
    private String category;

    @NotNull(message = "Indique el importe.")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero.")
    @Digits(integer = 10, fraction = 2, message = "Use hasta 10 enteros y 2 decimales.")
    private BigDecimal amount;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
    private String description;

    @NotNull(message = "Indique la fecha del movimiento.")
    @PastOrPresent(message = "La fecha del movimiento no puede ser futura.")
    @Builder.Default
    private LocalDate transactionDate = LocalDate.now();

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }
}
